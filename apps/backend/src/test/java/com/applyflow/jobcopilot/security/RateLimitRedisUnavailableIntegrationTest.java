package com.applyflow.jobcopilot.security;

import com.applyflow.jobcopilot.shared.infrastructure.ratelimit.RateLimitService;
import com.applyflow.jobcopilot.shared.infrastructure.ratelimit.RateLimitUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.rate-limit.redis-enabled=true",
        "security.rate-limit.fallback-enabled=false"
})
class RateLimitRedisUnavailableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimitService rateLimitService;

    @Test
    void shouldReturn503WhenRateLimitBackendIsUnavailableAndFallbackDisabled() throws Exception {
        when(rateLimitService.evaluate(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RateLimitUnavailableException("redis unavailable"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.local\",\"password\":\"Password123!\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_UNAVAILABLE"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-RateLimit-Mode", "unavailable"))
                .andExpect(header().string("X-RateLimit-Policy", "auth-login"));
    }
}
