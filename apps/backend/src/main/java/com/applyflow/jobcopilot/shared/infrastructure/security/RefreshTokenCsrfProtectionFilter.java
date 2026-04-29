package com.applyflow.jobcopilot.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class RefreshTokenCsrfProtectionFilter extends OncePerRequestFilter {
    public static final String CSRF_HEADER = "X-ApplyFlow-CSRF";
    private static final String CSRF_HEADER_VALUE = "1";
    private static final String REFRESH_COOKIE = "refresh_token";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (requiresHeaderProtection(request) && !hasValidHeader(request)) {
            reject(response, (String) request.getAttribute("correlationId"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresHeaderProtection(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && hasRefreshCookie(request)
                && ("/api/v1/auth/refresh".equals(request.getRequestURI())
                || "/api/v1/auth/logout".equals(request.getRequestURI()));
    }

    private boolean hasRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValidHeader(HttpServletRequest request) {
        return CSRF_HEADER_VALUE.equals(request.getHeader(CSRF_HEADER));
    }

    private void reject(HttpServletResponse response, String correlationId) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + OffsetDateTime.now()
                + "\",\"errorCode\":\"FORBIDDEN\",\"message\":\"Acesso negado\",\"correlationId\":"
                + (correlationId == null ? "null" : "\"" + correlationId + "\"")
                + "}");
    }
}
