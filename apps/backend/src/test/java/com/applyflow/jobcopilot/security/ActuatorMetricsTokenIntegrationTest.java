package com.applyflow.jobcopilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.actuator.metrics-token=test-metrics-token"
})
class ActuatorMetricsTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldDenyPrometheusWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowPrometheusWithValidMetricsToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("X-Actuator-Token", "test-metrics-token"))
                .andExpect(status().isOk());
    }
}
