package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

import com.applyflow.jobcopilot.shared.application.audit.AuditEventService;
import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.shared.infrastructure.observability.EndpointTagResolver;
import com.applyflow.jobcopilot.shared.infrastructure.security.SecurityProperties;
import com.applyflow.jobcopilot.shared.interfaces.http.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;
    private final SecurityProperties securityProperties;
    private final AuditEventService auditEventService;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService,
                           SecurityProperties securityProperties,
                           AuditEventService auditEventService,
                           OperationalMetricsService operationalMetricsService,
                           OperationalEventLogger operationalEventLogger,
                           ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.securityProperties = securityProperties;
        this.auditEventService = auditEventService;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitPolicy policy = resolvePolicy(request.getRequestURI(), request.getMethod());
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID actorId = null;
        String userPart = "anonymous";
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof AuthenticatedUser user) {
            actorId = user.userId();
            userPart = user.userId().toString();
        }

        String clientIp = resolveClientIp(request);
        String key = "ip:" + clientIp + "|uid:" + userPart;
        String endpointTag = EndpointTagResolver.resolve(request.getMethod(), request.getRequestURI());

        RateLimitDecision decision;
        try {
            decision = rateLimitService.evaluate(key, policy.policyName(), policy.limit(), policy.windowSeconds());
        } catch (RateLimitUnavailableException ex) {
            String correlationId = (String) request.getAttribute("correlationId");
            log.warn("rate_limit.unavailable policy={} correlationId={} fallbackEnabled={}",
                    policy.policyName(), correlationId, securityProperties.getRateLimit().isFallbackEnabled());
            response.setHeader("X-RateLimit-Policy", policy.policyName());
            response.setHeader("X-RateLimit-Mode", "unavailable");
            ApiErrorResponse error = new ApiErrorResponse(OffsetDateTime.now(), "RATE_LIMIT_UNAVAILABLE", "Servico temporariamente indisponivel", correlationId);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            operationalMetricsService.recordRateLimitDecision(policy.policyName(), "unavailable", "unavailable");
            operationalEventLogger.emit("rate_limit_unavailable", "WARN", actorId, endpointTag, "unavailable", policy.policyName());
            auditEventService.log(actorId, "RATE_LIMIT_UNAVAILABLE", "http_endpoint", null, policy.policyName(), request.getMethod() + ":" + request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetEpochSeconds()));
        response.setHeader("X-RateLimit-Policy", policy.policyName());
        response.setHeader("X-RateLimit-Mode", decision.mode());
        operationalMetricsService.recordRateLimitDecision(policy.policyName(), decision.allowed() ? "allowed" : "blocked", decision.mode());
        if ("in-memory-fallback".equalsIgnoreCase(decision.mode()) && securityProperties.getRateLimit().isRedisEnabled()) {
            operationalMetricsService.recordRateLimitFallback(policy.policyName());
            operationalEventLogger.emit("rate_limit_fallback_activated", "WARN", actorId, endpointTag, "fallback", policy.policyName());
        }

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String correlationId = (String) request.getAttribute("correlationId");
            ApiErrorResponse error = new ApiErrorResponse(OffsetDateTime.now(), "RATE_LIMIT_EXCEEDED", "Muitas requisicoes", correlationId);
            objectMapper.writeValue(response.getOutputStream(), error);
            auditEventService.log(actorId, "RATE_LIMIT_EXCEEDED", "http_endpoint", null, policy.policyName(), request.getMethod() + ":" + request.getRequestURI());
            operationalEventLogger.emit("rate_limit_blocked", "WARN", actorId, endpointTag, "blocked", policy.policyName());
            log.info("rate_limit.exceeded policy={} mode={} actorPresent={} ip={} correlationId={}",
                    policy.policyName(), decision.mode(), actorId != null, clientIp, correlationId);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private RateLimitPolicy resolvePolicy(String path, String method) {
        SecurityProperties.RateLimit cfg = securityProperties.getRateLimit();
        int window = cfg.getWindowSeconds();
        if ("POST".equals(method) && "/api/v1/auth/login".equals(path)) {
            return new RateLimitPolicy("auth-login", cfg.getLoginLimit(), window);
        }
        if ("POST".equals(method) && "/api/v1/auth/refresh".equals(path)) {
            return new RateLimitPolicy("auth-refresh", cfg.getRefreshLimit(), window);
        }
        if ("POST".equals(method) && "/api/v1/resumes".equals(path)) {
            return new RateLimitPolicy("resume-upload", cfg.getResumeUploadLimit(), window);
        }
        if ("POST".equals(method) && path.matches("^/api/v1/resumes/[^/]+/variants$")) {
            return new RateLimitPolicy("resume-variant-create", cfg.getResumeVariantLimit(), window);
        }
        if ("POST".equals(method) && "/api/v1/applications/drafts".equals(path)) {
            return new RateLimitPolicy("application-draft-create", cfg.getApplicationDraftLimit(), window);
        }
        if ("POST".equals(method) && "/api/v1/applications/drafts/assisted".equals(path)) {
            return new RateLimitPolicy("application-draft-create", cfg.getApplicationDraftLimit(), window);
        }
        if ("GET".equals(method) && "/api/v1/vacancies".equals(path)) {
            return new RateLimitPolicy("vacancies-read", cfg.getVacancyReadLimit(), window);
        }
        if ("GET".equals(method) && "/api/v1/job-search-preferences".equals(path)) {
            return new RateLimitPolicy("job-search-preferences-read", cfg.getVacancyReadLimit(), window);
        }
        if (("POST".equals(method) && "/api/v1/job-search-preferences".equals(path))
                || ("PATCH".equals(method) && path.matches("^/api/v1/job-search-preferences/[^/]+$"))) {
            return new RateLimitPolicy("job-search-preferences-write", cfg.getApplicationDraftLimit(), window);
        }
        if ("POST".equals(method) && "/api/v1/matches".equals(path)) {
            return new RateLimitPolicy("matches-analyze", cfg.getMatchAnalysisLimit(), window);
        }
        if ("GET".equals(method) && path.matches("^/api/v1/matches/vacancy/[^/]+$")) {
            return new RateLimitPolicy("matches-analyze", cfg.getMatchAnalysisLimit(), window);
        }
        if ("GET".equals(method) && path.matches("^/api/v1/matches/vacancy/[^/]+/summary$")) {
            return new RateLimitPolicy("matches-analyze", cfg.getMatchAnalysisLimit(), window);
        }
        if ("GET".equals(method) && path.matches("^/api/v1/matches/[^/]+$")) {
            return new RateLimitPolicy("matches-analyze", cfg.getMatchAnalysisLimit(), window);
        }
        if ("POST".equals(method) && path.matches("^/api/v1/ai/matches/[^/]+/(enrichment|cv-improvement|application-draft)$")) {
            return new RateLimitPolicy("ai-enrichment", cfg.getAiEnrichmentLimit(), window);
        }
        return null;
    }

    private record RateLimitPolicy(String policyName, int limit, int windowSeconds) {
    }
}
