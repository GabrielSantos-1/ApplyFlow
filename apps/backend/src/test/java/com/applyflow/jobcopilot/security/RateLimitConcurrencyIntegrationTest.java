package com.applyflow.jobcopilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.rate-limit.redis-enabled=false",
        "security.rate-limit.fallback-enabled=true",
        "security.rate-limit.login-limit=3",
        "security.rate-limit.window-seconds=60"
})
class RateLimitConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldBlockExcessConcurrentLoginAttempts() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        String payload = "{\"email\":\"concurrency@test.local\",\"password\":\"WrongPassword123!\"}";
        Callable<Integer> call = () -> mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn()
                .getResponse()
                .getStatus();

        List<Future<Integer>> futures = new ArrayList<>();
        IntStream.range(0, 10).forEach(i -> futures.add(executor.submit(call)));

        int blocked = 0;
        int forbidden = 0;
        for (Future<Integer> future : futures) {
            int status = future.get(10, TimeUnit.SECONDS);
            if (status == 429) blocked++;
            if (status == 403) forbidden++;
        }
        executor.shutdownNow();

        assertTrue(blocked >= 1, "Expected at least one 429 under burst");
        assertTrue(forbidden >= 1, "Expected at least one pre-limit 403");
    }
}
