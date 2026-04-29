package com.applyflow.jobcopilot.security;

import com.applyflow.jobcopilot.auth.infrastructure.persistence.entity.UserJpaEntity;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.applyflow.jobcopilot.shared.infrastructure.security.RefreshTokenCsrfProtectionFilter;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.rate-limit.login-limit=1000",
        "security.rate-limit.refresh-limit=1000",
        "security.rate-limit.resume-upload-limit=1000",
        "security.rate-limit.resume-variant-limit=1000",
        "security.rate-limit.application-draft-limit=1000",
        "security.rate-limit.vacancy-read-limit=1000",
        "security.rate-limit.match-analysis-limit=1000",
        "security.rate-limit.ai-enrichment-limit=1000"
})
class SecurityAuthorizationIntegrationTest {
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
    void protectedRouteWithoutTokenMustReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTokenMustReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/resumes").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = {"GUEST"})
    void insufficientRoleMustReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void nonAdminMustNotAccessPrometheusEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void nonAdminMustNotTriggerVacancyIngestion() throws Exception {
        mockMvc.perform(post("/api/v1/vacancies/ingestion/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"remotive\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void nonAdminMustNotTriggerAdminVacancyIngestion() throws Exception {
        mockMvc.perform(post("/api/v1/admin/vacancies/ingestion/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"remotive\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void anonymousMustNotAccessIngestionOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ingestion/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void nonAdminMustNotAccessIngestionOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ingestion/overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void adminCanAccessPrometheusEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    void ownershipMustBlockResumeAndVariantCrossUserAccess() throws Exception {
        LoginSession owner = login(seedUser("owner-resume@test.local", "Password123!", "USER"));
        LoginSession stranger = login(seedUser("stranger-resume@test.local", "Password123!", "USER"));

        UUID vacancyId = seedVacancy("Java Backend Ownership Resume");

        String resumeBody = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(owner.accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"title\":\"Resume Owner\"," +
                                "\"sourceFileName\":\"owner.pdf\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID resumeId = UUID.fromString(objectMapper.readTree(resumeBody).get("id").asText());

        mockMvc.perform(get("/api/v1/resumes/{id}", resumeId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/resumes/{id}/variants", resumeId)
                        .header("Authorization", bearer(stranger.accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"vacancyId\":\"" + vacancyId + "\"," +
                                "\"variantLabel\":\"Cross attempt\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownershipMustProtectApplicationsAndSanitizeText() throws Exception {
        LoginSession owner = login(seedUser("owner-app@test.local", "Password123!", "USER"));
        LoginSession stranger = login(seedUser("stranger-app@test.local", "Password123!", "USER"));

        UUID vacancyId = seedVacancy("Java Backend Ownership Application");

        UUID resumeId = createResume(owner.accessToken, "App Resume", "app.pdf");
        UUID variantId = createVariant(owner.accessToken, resumeId, vacancyId, "Var 1");

        String draftBody = mockMvc.perform(post("/api/v1/applications/drafts")
                        .header("Authorization", bearer(owner.accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"vacancyId\":\"" + vacancyId + "\"," +
                                "\"resumeVariantId\":\"" + variantId + "\"," +
                                "\"messageDraft\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageDraft").value("&lt;script&gt;alert(1)&lt;/script&gt;"))
                .andReturn().getResponse().getContentAsString();

        UUID draftId = UUID.fromString(objectMapper.readTree(draftBody).get("id").asText());

        mockMvc.perform(get("/api/v1/applications/{id}", draftId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/applications/{id}/tracking", draftId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/applications/{id}/status", draftId)
                        .header("Authorization", bearer(owner.accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"status\":\"INTERVIEW\"," +
                                "\"notes\":\"invalid transition\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void ownershipMustApplyToMatches() throws Exception {
        LoginSession owner = login(seedUser("owner-match@test.local", "Password123!", "USER"));
        LoginSession stranger = login(seedUser("stranger-match@test.local", "Password123!", "USER"));

        UUID vacancyId = seedVacancy("Java Backend Ownership Match");

        UUID ownerResume = createResume(owner.accessToken, "Owner Match Resume", "owner-match.pdf");
        createVariant(owner.accessToken, ownerResume, vacancyId, "owner-var");

        createResume(stranger.accessToken, "Stranger Match Resume", "stranger-match.pdf");

        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(owner.accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"vacancyId\":\"" + vacancyId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.recommendation").isString());

        mockMvc.perform(get("/api/v1/matches/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(owner.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.recommendation").isString())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.gaps").isArray());

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(owner.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.recommendation").isString())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.gaps").isArray());

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}/summary", vacancyId)
                        .header("Authorization", bearer(owner.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.recommendation").isString());

        mockMvc.perform(get("/api/v1/matches/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("MISSING_VARIANT"))
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.recommendation").isEmpty())
                .andExpect(jsonPath("$.strengths", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.gaps", Matchers.hasSize(0)));

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("MISSING_VARIANT"))
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.recommendation").isEmpty())
                .andExpect(jsonPath("$.strengths", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.gaps", Matchers.hasSize(0)));

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}/summary", vacancyId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("MISSING_VARIANT"))
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.recommendation").isEmpty());
    }

    @Test
    void ownershipMustApplyToAiEnrichmentRoutes() throws Exception {
        LoginSession owner = login(seedUser("owner-ai@test.local", "Password123!", "USER"));
        LoginSession stranger = login(seedUser("stranger-ai@test.local", "Password123!", "USER"));

        UUID vacancyId = seedVacancy("Java Backend Ownership AI");

        UUID ownerResume = createResume(owner.accessToken, "Owner AI Resume", "owner-ai.pdf");
        createVariant(owner.accessToken, ownerResume, vacancyId, "owner-ai-var");

        createResume(stranger.accessToken, "Stranger AI Resume", "stranger-ai.pdf");

        mockMvc.perform(post("/api/v1/ai/matches/{vacancyId}/enrichment", vacancyId)
                        .header("Authorization", bearer(owner.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deterministicScore").exists());

        mockMvc.perform(post("/api/v1/ai/matches/{vacancyId}/enrichment", vacancyId)
                        .header("Authorization", bearer(stranger.accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void matchingEndpointsMustExposeStatefulFlowAndSummaryConsistency() throws Exception {
        LoginSession user = login(seedUser("stateful-match@test.local", "Password123!", "USER"));
        UUID vacancyId = seedVacancy("Java Backend Stateful Match");

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(user.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("MISSING_RESUME"))
                .andExpect(jsonPath("$.hasResumeContext").value(false))
                .andExpect(jsonPath("$.hasVariantContext").value(false))
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.recommendation").isEmpty());

        UUID resumeId = createResume(user.accessToken, "Stateful Resume", "stateful.pdf");

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(user.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("MISSING_VARIANT"))
                .andExpect(jsonPath("$.hasResumeContext").value(true))
                .andExpect(jsonPath("$.hasVariantContext").value(false))
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.recommendation").isEmpty());

        createVariant(user.accessToken, resumeId, vacancyId, "stateful-var");

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(user.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NOT_GENERATED"))
                .andExpect(jsonPath("$.hasResumeContext").value(true))
                .andExpect(jsonPath("$.hasVariantContext").value(true))
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.recommendation").isEmpty());

        String generatedBody = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(user.accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"vacancyId\":\"" + vacancyId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.recommendation").isString())
                .andReturn().getResponse().getContentAsString();

        int generatedScore = objectMapper.readTree(generatedBody).get("score").asInt();
        String generatedRecommendation = objectMapper.readTree(generatedBody).get("recommendation").asText();

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}", vacancyId)
                        .header("Authorization", bearer(user.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").value(generatedScore))
                .andExpect(jsonPath("$.recommendation").value(generatedRecommendation));

        mockMvc.perform(get("/api/v1/matches/vacancy/{vacancyId}/summary", vacancyId)
                        .header("Authorization", bearer(user.accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("GENERATED"))
                .andExpect(jsonPath("$.score").value(generatedScore))
                .andExpect(jsonPath("$.recommendation").value(generatedRecommendation))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test
    void invalidVacancySortMustBeRejected() throws Exception {
        LoginSession owner = login(seedUser("owner-vacancy-filter@test.local", "Password123!", "USER"));

        mockMvc.perform(get("/api/v1/vacancies")
                        .header("Authorization", bearer(owner.accessToken))
                        .param("sortBy", "drop table"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void invalidUuidOnMatchPathMustNotReturn500() throws Exception {
        mockMvc.perform(get("/api/v1/matches/not-a-uuid"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void refreshReplayMustBeRejected() throws Exception {
        String email = "owner-refresh@test.local";
        seedUser(email, "Password123!", "USER");

        LoginSession login = login(email);

        String firstRefreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(RefreshTokenCsrfProtectionFilter.CSRF_HEADER, "1")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", login.refreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");

        String rotatedRefresh = extractRefreshToken(firstRefreshResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(RefreshTokenCsrfProtectionFilter.CSRF_HEADER, "1")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", login.refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(RefreshTokenCsrfProtectionFilter.CSRF_HEADER, "1")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", rotatedRefresh)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithCookieWithoutCsrfHeaderMustFail() throws Exception {
        String email = "csrf-refresh-missing@test.local";
        seedUser(email, "Password123!", "USER");
        LoginSession login = login(email);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", login.refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void refreshWithCookieAndCsrfHeaderMustPass() throws Exception {
        String email = "csrf-refresh-present@test.local";
        seedUser(email, "Password123!", "USER");
        LoginSession login = login(email);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(RefreshTokenCsrfProtectionFilter.CSRF_HEADER, "1")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", login.refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    void logoutWithCookieWithoutCsrfHeaderMustFail() throws Exception {
        String email = "csrf-logout-missing@test.local";
        seedUser(email, "Password123!", "USER");
        LoginSession login = login(email);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(login.accessToken))
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", login.refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void logoutWithCookieAndCsrfHeaderMustPass() throws Exception {
        String email = "csrf-logout-present@test.local";
        seedUser(email, "Password123!", "USER");
        LoginSession login = login(email);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(login.accessToken))
                        .header(RefreshTokenCsrfProtectionFilter.CSRF_HEADER, "1")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", login.refreshToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginMustNotRequireCsrfHeader() throws Exception {
        seedUser("csrf-login@test.local", "Password123!", "USER");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"email\":\"csrf-login@test.local\"," +
                                "\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
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

    private LoginSession login(UserJpaEntity user) throws Exception {
        return login(user.getEmail());
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
