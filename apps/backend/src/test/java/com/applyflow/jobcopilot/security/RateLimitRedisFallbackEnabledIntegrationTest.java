package com.applyflow.jobcopilot.security;

import com.applyflow.jobcopilot.shared.infrastructure.ratelimit.RedisRateLimitService;
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
        "security.rate-limit.fallback-enabled=true",
        "security.rate-limit.login-limit=2",
        "security.rate-limit.window-seconds=60"
})
class RateLimitRedisFallbackEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisRateLimitService redisRateLimitService;

    @Test
    void shouldUseFallbackAndKeepEndpointOperationalWhenRedisFails() throws Exception {
        when(redisRateLimitService.evaluate(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RateLimitUnavailableException("redis unavailable"));

        String payload = "{\"email\":\"fallback@test.local\",\"password\":\"WrongPassword123!\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-RateLimit-Mode", "in-memory-fallback"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-RateLimit-Mode", "in-memory-fallback"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(header().string("X-RateLimit-Mode", "in-memory-fallback"))
                .andExpect(header().string("X-RateLimit-Policy", "auth-login"));
    }
}
