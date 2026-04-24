package com.applyflow.jobcopilot.shared.infrastructure.security;

import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.infrastructure.observability.EndpointTagResolver;
import com.applyflow.jobcopilot.shared.interfaces.http.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;

    public RestAccessDeniedHandler(ObjectMapper objectMapper,
                                   OperationalMetricsService operationalMetricsService,
                                   OperationalEventLogger operationalEventLogger) {
        this.objectMapper = objectMapper;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String endpoint = EndpointTagResolver.resolve(request.getMethod(), request.getRequestURI());
        operationalMetricsService.recordAuthorization("forbidden", endpoint);
        operationalEventLogger.emit("http_forbidden", "WARN", null, endpoint, "forbidden", "access-denied");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(OffsetDateTime.now(), "FORBIDDEN", "Acesso negado", (String) request.getAttribute("correlationId"));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
