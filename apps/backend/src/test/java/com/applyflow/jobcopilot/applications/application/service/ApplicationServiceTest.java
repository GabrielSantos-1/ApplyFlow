package com.applyflow.jobcopilot.applications.application.service;

import com.applyflow.jobcopilot.applications.application.dto.request.CreateApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.UpdateApplicationTrackingStatusRequest;
import com.applyflow.jobcopilot.applications.domain.ApplicationStatus;
import com.applyflow.jobcopilot.applications.domain.TrackingStage;
import com.applyflow.jobcopilot.applications.infrastructure.persistence.entity.ApplicationDraftJpaEntity;
import com.applyflow.jobcopilot.applications.infrastructure.persistence.entity.ApplicationTrackingJpaEntity;
import com.applyflow.jobcopilot.applications.infrastructure.persistence.repository.ApplicationDraftJpaRepository;
import com.applyflow.jobcopilot.applications.infrastructure.persistence.repository.ApplicationTrackingJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeVariantJpaRepository;
import com.applyflow.jobcopilot.shared.application.audit.AuditEventService;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {
    @Test
    void invalidTransitionShouldBeRejected() {
        var draftRepo = mock(ApplicationDraftJpaRepository.class);
        var trackingRepo = mock(ApplicationTrackingJpaRepository.class);
        var authCtx = mock(AuthContextService.class);
        when(authCtx.requireAuthenticatedUser()).thenReturn(new AuthenticatedUser(UUID.randomUUID(), "u@t.com", com.applyflow.jobcopilot.auth.domain.UserRole.USER));

        ApplicationDraftJpaEntity draft = new ApplicationDraftJpaEntity();
        draft.setId(UUID.randomUUID());
        draft.setUserId(authCtx.requireAuthenticatedUser().userId());
        draft.setStatus(ApplicationStatus.DRAFT.name());
        draft.setCreatedAt(OffsetDateTime.now());
        draft.setUpdatedAt(OffsetDateTime.now());
        when(draftRepo.findByIdAndUserId(any(), any())).thenReturn(Optional.of(draft));

        StubApplicationService service = new StubApplicationService(
                draftRepo,
                trackingRepo,
                mock(ResumeJpaRepository.class),
                mock(ResumeVariantJpaRepository.class),
                mock(VacancyJpaRepository.class),
                authCtx,
                new TextSanitizer(),
                mock(AuditEventService.class),
                mock(OperationalMetricsService.class),
                mock(OperationalEventLogger.class)
        );
        assertThrows(BadRequestException.class, () -> service.updateStatus(draft.getId(), new UpdateApplicationTrackingStatusRequest(ApplicationStatus.INTERVIEW, "x")));
    }

    @Test
    void createDraftShouldRejectVariantFromDifferentVacancy() {
        var draftRepo = mock(ApplicationDraftJpaRepository.class);
        var trackingRepo = mock(ApplicationTrackingJpaRepository.class);
        var resumeRepo = mock(ResumeJpaRepository.class);
        var variantRepo = mock(ResumeVariantJpaRepository.class);
        var vacancyRepo = mock(VacancyJpaRepository.class);
        var authCtx = mock(AuthContextService.class);

        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        UUID otherVacancyId = UUID.randomUUID();

        when(authCtx.requireAuthenticatedUser()).thenReturn(new AuthenticatedUser(userId, "u@t.com", com.applyflow.jobcopilot.auth.domain.UserRole.USER));

        when(vacancyRepo.findById(vacancyId)).thenReturn(Optional.of(new com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity()));

        ResumeVariantJpaEntity variant = new ResumeVariantJpaEntity();
        variant.setId(variantId);
        variant.setResumeId(resumeId);
        variant.setVacancyId(otherVacancyId);
        when(variantRepo.findById(variantId)).thenReturn(Optional.of(variant));

        ResumeJpaEntity resume = new ResumeJpaEntity();
        resume.setId(resumeId);
        resume.setUserId(userId);
        when(resumeRepo.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(resume));

        StubApplicationService service = new StubApplicationService(
                draftRepo,
                trackingRepo,
                resumeRepo,
                variantRepo,
                vacancyRepo,
                authCtx,
                new TextSanitizer(),
                mock(AuditEventService.class),
                mock(OperationalMetricsService.class),
                mock(OperationalEventLogger.class)
        );

        CreateApplicationDraftRequest request = new CreateApplicationDraftRequest(vacancyId, variantId, "msg");
        assertThrows(BadRequestException.class, () -> service.createDraft(request));
    }

    @Test
    void listTrackingMustRespectOwnershipAndReturnOrderedTimeline() {
        var draftRepo = mock(ApplicationDraftJpaRepository.class);
        var trackingRepo = mock(ApplicationTrackingJpaRepository.class);
        var authCtx = mock(AuthContextService.class);

        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(authCtx.requireAuthenticatedUser()).thenReturn(new AuthenticatedUser(userId, "u@t.com", com.applyflow.jobcopilot.auth.domain.UserRole.USER));

        ApplicationDraftJpaEntity draft = new ApplicationDraftJpaEntity();
        draft.setId(draftId);
        draft.setUserId(userId);
        when(draftRepo.findByIdAndUserId(draftId, userId)).thenReturn(Optional.of(draft));

        ApplicationTrackingJpaEntity created = new ApplicationTrackingJpaEntity();
        created.setId(UUID.randomUUID());
        created.setApplicationDraftId(draftId);
        created.setStage(TrackingStage.CREATED.name());
        created.setNotes("draft-created");
        created.setCreatedAt(OffsetDateTime.parse("2026-04-23T10:00:00Z"));

        ApplicationTrackingJpaEntity submitted = new ApplicationTrackingJpaEntity();
        submitted.setId(UUID.randomUUID());
        submitted.setApplicationDraftId(draftId);
        submitted.setStage(TrackingStage.SUBMITTED.name());
        submitted.setNotes("manual-apply");
        submitted.setCreatedAt(OffsetDateTime.parse("2026-04-23T11:00:00Z"));

        when(trackingRepo.findByApplicationDraftIdOrderByCreatedAtAsc(draftId)).thenReturn(List.of(created, submitted));

        StubApplicationService service = new StubApplicationService(
                draftRepo,
                trackingRepo,
                mock(ResumeJpaRepository.class),
                mock(ResumeVariantJpaRepository.class),
                mock(VacancyJpaRepository.class),
                authCtx,
                new TextSanitizer(),
                mock(AuditEventService.class),
                mock(OperationalMetricsService.class),
                mock(OperationalEventLogger.class)
        );

        var timeline = service.listTracking(draftId);

        assertEquals(2, timeline.size());
        assertEquals(TrackingStage.CREATED, timeline.get(0).stage());
        assertEquals(TrackingStage.SUBMITTED, timeline.get(1).stage());
    }

    @Test
    void listTrackingMustHandleLegacyOrUnknownStageWithout500() {
        var draftRepo = mock(ApplicationDraftJpaRepository.class);
        var trackingRepo = mock(ApplicationTrackingJpaRepository.class);
        var authCtx = mock(AuthContextService.class);

        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(authCtx.requireAuthenticatedUser()).thenReturn(new AuthenticatedUser(userId, "u@t.com", com.applyflow.jobcopilot.auth.domain.UserRole.USER));

        ApplicationDraftJpaEntity draft = new ApplicationDraftJpaEntity();
        draft.setId(draftId);
        draft.setUserId(userId);
        when(draftRepo.findByIdAndUserId(draftId, userId)).thenReturn(Optional.of(draft));

        ApplicationTrackingJpaEntity legacyApplied = new ApplicationTrackingJpaEntity();
        legacyApplied.setId(UUID.randomUUID());
        legacyApplied.setApplicationDraftId(draftId);
        legacyApplied.setStage("APPLIED");
        legacyApplied.setNotes("legacy-applied");
        legacyApplied.setCreatedAt(OffsetDateTime.parse("2026-04-23T10:00:00Z"));

        ApplicationTrackingJpaEntity unknown = new ApplicationTrackingJpaEntity();
        unknown.setId(UUID.randomUUID());
        unknown.setApplicationDraftId(draftId);
        unknown.setStage("SOMETHING_ELSE");
        unknown.setNotes("legacy-unknown");
        unknown.setCreatedAt(OffsetDateTime.parse("2026-04-23T11:00:00Z"));

        when(trackingRepo.findByApplicationDraftIdOrderByCreatedAtAsc(draftId)).thenReturn(List.of(legacyApplied, unknown));

        StubApplicationService service = new StubApplicationService(
                draftRepo,
                trackingRepo,
                mock(ResumeJpaRepository.class),
                mock(ResumeVariantJpaRepository.class),
                mock(VacancyJpaRepository.class),
                authCtx,
                new TextSanitizer(),
                mock(AuditEventService.class),
                mock(OperationalMetricsService.class),
                mock(OperationalEventLogger.class)
        );

        var timeline = service.listTracking(draftId);

        assertEquals(2, timeline.size());
        assertEquals(TrackingStage.SUBMITTED, timeline.get(0).stage());
        assertEquals(TrackingStage.CLOSED, timeline.get(1).stage());
    }
}
