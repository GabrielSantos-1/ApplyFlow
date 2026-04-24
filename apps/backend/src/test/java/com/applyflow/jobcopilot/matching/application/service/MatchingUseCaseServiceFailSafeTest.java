package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.auth.domain.UserRole;
import com.applyflow.jobcopilot.matching.domain.MatchState;
import com.applyflow.jobcopilot.matching.infrastructure.persistence.entity.MatchResultJpaEntity;
import com.applyflow.jobcopilot.matching.infrastructure.persistence.repository.MatchResultJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.CandidateProfileJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeVariantJpaRepository;
import com.applyflow.jobcopilot.shared.application.audit.AuditEventService;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchingUseCaseServiceFailSafeTest {

    @Test
    void getByVacancyShouldReturnMissingResumeWhenStoredMatchIsInconsistentAndNoResumeExists() {
        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        var service = buildService(userId, vacancyId);

        MatchResultJpaEntity malformed = new MatchResultJpaEntity();
        malformed.setId(UUID.randomUUID());
        malformed.setUserId(userId);
        malformed.setVacancyId(vacancyId);
        malformed.setScore(null);
        malformed.setRecommendation(null);

        when(service.matchResultRepository().findTopByUserIdAndVacancyIdOrderByCreatedAtDesc(userId, vacancyId))
                .thenReturn(Optional.of(malformed));
        when(service.resumeRepository().findFirstByUserIdAndBaseTrue(userId)).thenReturn(Optional.empty());
        when(service.resumeRepository().findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        var response = service.sut().getByVacancy(vacancyId);
        assertEquals(MatchState.MISSING_RESUME, response.state());
    }

    @Test
    void getByVacancyShouldReturnNotGeneratedWhenStoredMatchIsInconsistentButContextExists() {
        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();

        var service = buildService(userId, vacancyId);

        MatchResultJpaEntity malformed = new MatchResultJpaEntity();
        malformed.setId(UUID.randomUUID());
        malformed.setUserId(userId);
        malformed.setVacancyId(vacancyId);
        malformed.setScore(null);
        malformed.setRecommendation("APPLY");

        ResumeJpaEntity resume = new ResumeJpaEntity();
        resume.setId(resumeId);
        resume.setUserId(userId);

        ResumeVariantJpaEntity variant = new ResumeVariantJpaEntity();
        variant.setId(UUID.randomUUID());
        variant.setResumeId(resumeId);
        variant.setVacancyId(vacancyId);

        when(service.matchResultRepository().findTopByUserIdAndVacancyIdOrderByCreatedAtDesc(userId, vacancyId))
                .thenReturn(Optional.of(malformed));
        when(service.resumeRepository().findFirstByUserIdAndBaseTrue(userId)).thenReturn(Optional.of(resume));
        when(service.variantRepository().findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(resumeId, vacancyId))
                .thenReturn(Optional.of(variant));

        var response = service.sut().getByVacancy(vacancyId);
        assertEquals(MatchState.NOT_GENERATED, response.state());
    }

    private TestDeps buildService(UUID userId, UUID vacancyId) {
        var matchScoringService = mock(com.applyflow.jobcopilot.matching.application.usecase.MatchScoringService.class);
        var matchingFeatureExtractorService = mock(MatchingFeatureExtractorService.class);
        var matchingExplanationService = mock(MatchingExplanationService.class);
        var vacancyRepository = mock(VacancyJpaRepository.class);
        var resumeRepository = mock(ResumeJpaRepository.class);
        var variantRepository = mock(ResumeVariantJpaRepository.class);
        var candidateProfileRepository = mock(CandidateProfileJpaRepository.class);
        var matchResultRepository = mock(MatchResultJpaRepository.class);
        var authContextService = mock(AuthContextService.class);
        var auditEventService = mock(AuditEventService.class);
        var operationalMetricsService = mock(OperationalMetricsService.class);

        VacancyJpaEntity vacancy = new VacancyJpaEntity();
        vacancy.setId(vacancyId);

        when(authContextService.requireAuthenticatedUser())
                .thenReturn(new AuthenticatedUser(userId, "user@test.local", UserRole.USER));
        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancy));

        MatchingUseCaseService sut = new MatchingUseCaseService(
                matchScoringService,
                matchingFeatureExtractorService,
                matchingExplanationService,
                vacancyRepository,
                resumeRepository,
                variantRepository,
                candidateProfileRepository,
                matchResultRepository,
                authContextService,
                auditEventService,
                operationalMetricsService,
                new ObjectMapper()
        );

        return new TestDeps(sut, matchResultRepository, resumeRepository, variantRepository);
    }

    private record TestDeps(
            MatchingUseCaseService sut,
            MatchResultJpaRepository matchResultRepository,
            ResumeJpaRepository resumeRepository,
            ResumeVariantJpaRepository variantRepository
    ) {
    }
}
