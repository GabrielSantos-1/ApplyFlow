package com.applyflow.jobcopilot.ai.application.service;

import com.applyflow.jobcopilot.ai.application.dto.MatchEnrichmentResponse;
import com.applyflow.jobcopilot.ai.application.port.AiTextGenerationPort;
import com.applyflow.jobcopilot.matching.application.dto.MatchAnalysisResponse;
import com.applyflow.jobcopilot.matching.application.usecase.MatchingUseCase;
import com.applyflow.jobcopilot.matching.domain.MatchRecommendation;
import com.applyflow.jobcopilot.matching.domain.MatchState;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.CandidateProfileJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.CandidateProfileJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeVariantJpaRepository;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AiEnrichmentServiceTest {

    @Test
    void shouldReturnFallbackWhenProviderFails() {
        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();

        MatchingUseCase matchingUseCase = mock(MatchingUseCase.class);
        VacancyJpaRepository vacancyRepository = mock(VacancyJpaRepository.class);
        ResumeJpaRepository resumeRepository = mock(ResumeJpaRepository.class);
        ResumeVariantJpaRepository variantRepository = mock(ResumeVariantJpaRepository.class);
        CandidateProfileJpaRepository profileRepository = mock(CandidateProfileJpaRepository.class);
        AuthContextService authContextService = mock(AuthContextService.class);
        AiTextGenerationPort aiProvider = mock(AiTextGenerationPort.class);
        OperationalMetricsService metrics = mock(OperationalMetricsService.class);

        VacancyJpaEntity vacancy = new VacancyJpaEntity();
        vacancy.setId(vacancyId);
        vacancy.setTitle("Backend Java");
        vacancy.setRequiredSkills("Java,Spring,Docker");
        vacancy.setRemote(true);
        vacancy.setCompany("ApplyFlow");

        ResumeJpaEntity resume = new ResumeJpaEntity();
        resume.setId(resumeId);
        resume.setUserId(userId);
        resume.setTitle("Senior Backend Resume");

        ResumeVariantJpaEntity variant = new ResumeVariantJpaEntity();
        variant.setId(UUID.randomUUID());
        variant.setResumeId(resumeId);
        variant.setVacancyId(vacancyId);
        variant.setVariantLabel("default");

        MatchAnalysisResponse deterministic = new MatchAnalysisResponse(
                vacancyId,
                68,
                MatchRecommendation.REVIEW,
                Map.of("stackPrincipal", 25, "seniority", 10),
                List.of("Stack principal aderente"),
                List.of("Requisitos obrigatorios ausentes: kafka"),
                List.of("kafka"),
                MatchState.GENERATED,
                "deterministic-v1",
                OffsetDateTime.now(),
                true,
                true
        );

        when(authContextService.requireAuthenticatedUser()).thenReturn(new AuthenticatedUser(userId, "user@test.local", null));
        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancy));
        when(resumeRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(resume));
        when(variantRepository.findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(resumeId, vacancyId)).thenReturn(Optional.of(variant));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(new CandidateProfileJpaEntity()));
        when(matchingUseCase.analyze(vacancyId)).thenReturn(deterministic);
        when(aiProvider.providerName()).thenReturn("openai-compatible");
        when(aiProvider.generate(any(), anyString(), anyString())).thenThrow(new RuntimeException("provider down"));

        AiEnrichmentService service = new AiEnrichmentService(
                matchingUseCase,
                vacancyRepository,
                resumeRepository,
                variantRepository,
                profileRepository,
                authContextService,
                new AiPromptFactory(new ObjectMapper(), new TextSanitizer()),
                aiProvider,
                new AiOutputValidator(new ObjectMapper(), new TextSanitizer()),
                new AiFallbackFactory(),
                metrics
        );

        MatchEnrichmentResponse response = service.enrichMatch(vacancyId);

        assertTrue(response.fallbackUsed());
        assertEquals(68, response.deterministicScore());
        assertEquals("REVIEW", response.deterministicRecommendation());
        verify(metrics).recordAiFallback(eq("match-enrichment"), anyString());
    }
}
