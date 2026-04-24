package com.applyflow.jobcopilot.security;

import com.applyflow.jobcopilot.auth.infrastructure.persistence.entity.UserJpaEntity;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.rate-limit.ai-enrichment-limit=2",
        "security.rate-limit.window-seconds=60",
        "security.rate-limit.redis-enabled=false",
        "security.rate-limit.fallback-enabled=true",
        "ai.enabled=false"
})
class AiRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private VacancyJpaRepository vacancyRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldBlockAiEndpointAfterLimitExceeded() throws Exception {
        String email = "ai-rate-limit@test.local";
        seedUser(email, "Password123!", "USER");
        LoginSession login = login(email);
        UUID vacancyId = seedVacancy("AI Endpoint Rate Limit");
        UUID resumeId = createResume(login.accessToken, "Resume AI", "resume-ai.pdf");
        createVariant(login.accessToken, resumeId, vacancyId, "ai-var");

        mockMvc.perform(post("/api/v1/ai/matches/{vacancyId}/enrichment", vacancyId)
                        .header("Authorization", bearer(login.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackUsed").exists());

        mockMvc.perform(post("/api/v1/ai/matches/{vacancyId}/enrichment", vacancyId)
                        .header("Authorization", bearer(login.accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ai/matches/{vacancyId}/enrichment", vacancyId)
                        .header("Authorization", bearer(login.accessToken)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(header().string("X-RateLimit-Policy", "ai-enrichment"));
    }

    private UUID createResume(String accessToken, String title, String sourceFileName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"title\":\"" + title + "\"," +
                                "\"sourceFileName\":\"" + sourceFileName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID createVariant(String accessToken, UUID resumeId, UUID vacancyId, String label) throws Exception {
        String body = mockMvc.perform(post("/api/v1/resumes/{id}/variants", resumeId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"vacancyId\":\"" + vacancyId + "\"," +
                                "\"variantLabel\":\"" + label + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID seedVacancy(String title) {
        VacancyJpaEntity vacancy = new VacancyJpaEntity();
        vacancy.setId(UUID.randomUUID());
        vacancy.setTitle(title);
        vacancy.setCompany("ApplyFlow");
        vacancy.setLocation("Sao Paulo");
        vacancy.setRemote(true);
        vacancy.setSeniority("senior");
        vacancy.setStatus(VacancyStatus.PUBLISHED.name());
        vacancy.setRequiredSkills("Java,Spring Boot,PostgreSQL");
        vacancy.setRawDescription("Backend role with Java and Spring");
        vacancy.setCreatedAt(OffsetDateTime.now());
        vacancy.setUpdatedAt(OffsetDateTime.now());
        vacancyRepository.save(vacancy);
        return vacancy.getId();
    }

    private UserJpaEntity seedUser(String email, String password, String role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            UserJpaEntity user = new UserJpaEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(role);
            user.setActive(true);
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            return userRepository.save(user);
        });
    }

    private LoginSession login(String email) throws Exception {
        String loginPayload = "{" +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"Password123!\"}";

        var response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        String accessToken = body.get("accessToken").asText();
        String refreshToken = extractRefreshToken(response.getHeader("Set-Cookie"));
        return new LoginSession(accessToken, refreshToken);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String extractRefreshToken(String setCookie) {
        if (setCookie == null) {
            throw new IllegalStateException("refresh cookie missing");
        }
        String prefix = "refresh_token=";
        int start = setCookie.indexOf(prefix);
        int end = setCookie.indexOf(';', start);
        if (start < 0) {
            throw new IllegalStateException("refresh cookie missing value");
        }
        return setCookie.substring(start + prefix.length(), end > 0 ? end : setCookie.length());
    }

    private record LoginSession(String accessToken, String refreshToken) {
    }
}

