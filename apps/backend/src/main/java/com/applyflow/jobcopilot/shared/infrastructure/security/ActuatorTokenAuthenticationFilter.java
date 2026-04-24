package com.applyflow.jobcopilot.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ActuatorTokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String TOKEN_HEADER = "X-Actuator-Token";

    private final SecurityProperties securityProperties;

    public ActuatorTokenAuthenticationFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isSensitiveActuatorEndpoint(request.getRequestURI()) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String expected = securityProperties.getActuator().getMetricsToken();
            String provided = request.getHeader(TOKEN_HEADER);
            if (matchesToken(expected, provided)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "actuator-scraper",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSensitiveActuatorEndpoint(String path) {
        return "/actuator/prometheus".equals(path)
                || "/actuator/metrics".equals(path)
                || (path != null && path.startsWith("/actuator/metrics/"));
    }

    private boolean matchesToken(String expected, String provided) {
        if (expected == null || expected.isBlank() || provided == null || provided.isBlank()) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}
