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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper,
                                        OperationalMetricsService operationalMetricsService,
                                        OperationalEventLogger operationalEventLogger) {
        this.objectMapper = objectMapper;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String endpoint = EndpointTagResolver.resolve(request.getMethod(), request.getRequestURI());
        operationalMetricsService.recordAuthorization("unauthorized", endpoint);
        operationalEventLogger.emit("http_unauthorized", "WARN", null, endpoint, "unauthorized", "authentication-required");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(OffsetDateTime.now(), "UNAUTHORIZED", "Credenciais invalidas", (String) request.getAttribute("correlationId"));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
