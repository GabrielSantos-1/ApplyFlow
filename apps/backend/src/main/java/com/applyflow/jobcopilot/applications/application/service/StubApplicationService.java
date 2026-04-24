package com.applyflow.jobcopilot.applications.application.service;

import com.applyflow.jobcopilot.applications.application.dto.request.CreateAssistedApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.CreateApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.UpdateApplicationTrackingStatusRequest;
import com.applyflow.jobcopilot.applications.application.dto.response.ApplicationDraftResponse;
import com.applyflow.jobcopilot.applications.application.dto.response.ApplicationTrackingEventResponse;
import com.applyflow.jobcopilot.applications.application.usecase.ApplicationUseCase;
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
import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.exception.NotFoundException;
import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class StubApplicationService implements ApplicationUseCase {
    private static final Logger log = LoggerFactory.getLogger(StubApplicationService.class);
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ApplicationStatus.DRAFT, Set.of(ApplicationStatus.READY_FOR_REVIEW, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.READY_FOR_REVIEW, Set.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.APPLIED, Set.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED, ApplicationStatus.OFFER, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.INTERVIEW, Set.of(ApplicationStatus.REJECTED, ApplicationStatus.OFFER, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OFFER, Set.of(ApplicationStatus.WITHDRAWN),
            ApplicationStatus.REJECTED, Set.of(),
            ApplicationStatus.WITHDRAWN, Set.of()
    );

    private final ApplicationDraftJpaRepository draftRepository;
    private final ApplicationTrackingJpaRepository trackingRepository;
    private final ResumeJpaRepository resumeRepository;
    private final ResumeVariantJpaRepository variantRepository;
    private final VacancyJpaRepository vacancyRepository;
    private final AuthContextService authContextService;
    private final TextSanitizer sanitizer;
    private final AuditEventService auditEventService;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;

    public StubApplicationService(ApplicationDraftJpaRepository draftRepository,
                                  ApplicationTrackingJpaRepository trackingRepository,
                                  ResumeJpaRepository resumeRepository,
                                  ResumeVariantJpaRepository variantRepository,
                                  VacancyJpaRepository vacancyRepository,
                                  AuthContextService authContextService,
                                  TextSanitizer sanitizer,
                                  AuditEventService auditEventService,
                                  OperationalMetricsService operationalMetricsService,
                                  OperationalEventLogger operationalEventLogger) {
        this.draftRepository = draftRepository;
        this.trackingRepository = trackingRepository;
        this.resumeRepository = resumeRepository;
        this.variantRepository = variantRepository;
        this.vacancyRepository = vacancyRepository;
        this.authContextService = authContextService;
        this.sanitizer = sanitizer;
        this.auditEventService = auditEventService;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
    }

    @Override
    public PageResponse<ApplicationDraftResponse> list(int page, int size) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        Page<ApplicationDraftJpaEntity> dbPage = draftRepository.findByUserId(actor.userId(), PageRequest.of(page, size));
        return new PageResponse<>(dbPage.getContent().stream().map(this::toResponse).toList(), dbPage.getNumber(), dbPage.getSize(), dbPage.getTotalElements(), dbPage.getTotalPages());
    }

    @Override
    @Transactional
    public ApplicationDraftResponse createDraft(CreateApplicationDraftRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        validateDraftOwnership(actor, request.vacancyId(), request.resumeVariantId());
        ApplicationDraftJpaEntity saved = persistDraft(actor, request.vacancyId(), request.resumeVariantId(), request.messageDraft());
        auditEventService.log(actor.userId(), "APPLICATION_DRAFT_CREATED", "application_draft", saved.getId(), null, saved.getStatus());
        operationalEventLogger.emit("application_draft_created", "INFO", actor.userId(), "applications.drafts.post", "success", saved.getId().toString());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationDraftResponse createDraftAssisted(CreateAssistedApplicationDraftRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        vacancyRepository.findById(request.vacancyId())
                .orElseThrow(() -> new NotFoundException("Vaga nao encontrada"));

        ResumeJpaEntity resume = resolveResume(actor, request.resumeId());
        ResumeVariantJpaEntity variant = variantRepository
                .findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(resume.getId(), request.vacancyId())
                .orElseGet(() -> createVariant(resume.getId(), request.vacancyId()));

        ApplicationDraftJpaEntity existing = draftRepository
                .findTopByUserIdAndVacancyIdOrderByCreatedAtDesc(actor.userId(), request.vacancyId())
                .orElse(null);

        if (existing != null) {
            operationalEventLogger.emit("application_draft_assisted", "INFO", actor.userId(), "applications.drafts.assisted.post", "reused", existing.getId().toString());
            return toResponse(existing);
        }

        ApplicationDraftJpaEntity saved = persistDraft(actor, request.vacancyId(), variant.getId(), request.messageDraft());
        auditEventService.log(actor.userId(), "APPLICATION_DRAFT_ASSISTED_CREATED", "application_draft", saved.getId(), null, saved.getStatus());
        operationalEventLogger.emit("application_draft_assisted", "INFO", actor.userId(), "applications.drafts.assisted.post", "created", saved.getId().toString());
        return toResponse(saved);
    }

    private ApplicationDraftJpaEntity persistDraft(AuthenticatedUser actor, UUID vacancyId, UUID resumeVariantId, String messageDraft) {
        ApplicationDraftJpaEntity draft = new ApplicationDraftJpaEntity();
        draft.setId(UUID.randomUUID());
        draft.setUserId(actor.userId());
        draft.setVacancyId(vacancyId);
        draft.setResumeVariantId(resumeVariantId);
        draft.setMessageDraft(sanitizer.sanitizeFreeText(messageDraft, 4000));
        draft.setStatus(ApplicationStatus.DRAFT.name());
        draft.setCreatedAt(OffsetDateTime.now());
        draft.setUpdatedAt(OffsetDateTime.now());
        ApplicationDraftJpaEntity saved = draftRepository.save(draft);

        ApplicationTrackingJpaEntity tracking = new ApplicationTrackingJpaEntity();
        tracking.setId(UUID.randomUUID());
        tracking.setApplicationDraftId(saved.getId());
        tracking.setStage(TrackingStage.CREATED.name());
        tracking.setNotes("draft-created");
        tracking.setCreatedAt(OffsetDateTime.now());
        tracking.setUpdatedAt(OffsetDateTime.now());
        trackingRepository.save(tracking);
        return saved;
    }

    private ResumeJpaEntity resolveResume(AuthenticatedUser actor, UUID requestedResumeId) {
        if (requestedResumeId != null) {
            return resumeRepository.findByIdAndUserId(requestedResumeId, actor.userId())
                    .orElseThrow(() -> new NotFoundException("Curriculo nao encontrado"));
        }
        return resumeRepository.findFirstByUserIdAndBaseTrue(actor.userId())
                .or(() -> resumeRepository.findTopByUserIdOrderByCreatedAtDesc(actor.userId()))
                .orElseThrow(() -> new NotFoundException("Nenhum curriculo encontrado para o usuario"));
    }

    private ResumeVariantJpaEntity createVariant(UUID resumeId, UUID vacancyId) {
        ResumeVariantJpaEntity variant = new ResumeVariantJpaEntity();
        variant.setId(UUID.randomUUID());
        variant.setResumeId(resumeId);
        variant.setVacancyId(vacancyId);
        variant.setVariantLabel("Variant assistida");
        variant.setStatus("DRAFT");
        variant.setCreatedAt(OffsetDateTime.now());
        variant.setUpdatedAt(OffsetDateTime.now());
        return variantRepository.save(variant);
    }

    private void validateDraftOwnership(AuthenticatedUser actor, UUID vacancyId, UUID resumeVariantId) {
        vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Vaga nao encontrada"));
        ResumeVariantJpaEntity variant = variantRepository.findById(resumeVariantId)
                .orElseThrow(() -> new NotFoundException("Variante de curriculo nao encontrada"));
        ResumeJpaEntity resume = resumeRepository.findByIdAndUserId(variant.getResumeId(), actor.userId())
                .orElseThrow(() -> new NotFoundException("Curriculo nao encontrado para usuario autenticado"));
        if (!vacancyId.equals(variant.getVacancyId())) {
            throw new BadRequestException("Variante nao pertence a vaga informada");
        }
        if (!resume.getId().equals(variant.getResumeId())) {
            throw new BadRequestException("Variante invalida");
        }
    }

    @Override
    @Transactional
    public ApplicationDraftResponse updateStatus(UUID id, UpdateApplicationTrackingStatusRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        ApplicationDraftJpaEntity draft = draftRepository.findByIdAndUserId(id, actor.userId())
                .orElseThrow(() -> new NotFoundException("Candidatura nao encontrada"));

        ApplicationStatus current = ApplicationStatus.valueOf(draft.getStatus());
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(request.status())) {
            operationalMetricsService.recordApplicationStatusTransition(current.name(), request.status().name(), "invalid");
            operationalEventLogger.emit("application_status_transition", "WARN", actor.userId(), "applications.status.patch", "invalid", current + "->" + request.status());
            throw new BadRequestException("Transicao de status invalida");
        }

        draft.setStatus(request.status().name());
        draft.setUpdatedAt(OffsetDateTime.now());
        ApplicationDraftJpaEntity updated = draftRepository.save(draft);

        ApplicationTrackingJpaEntity tracking = new ApplicationTrackingJpaEntity();
        tracking.setId(UUID.randomUUID());
        tracking.setApplicationDraftId(updated.getId());
        tracking.setStage(toStage(request.status()).name());
        tracking.setNotes(sanitizer.sanitizeFreeText(request.notes(), 2000));
        tracking.setCreatedAt(OffsetDateTime.now());
        tracking.setUpdatedAt(OffsetDateTime.now());
        trackingRepository.save(tracking);

        auditEventService.log(actor.userId(), "APPLICATION_STATUS_UPDATED", "application_draft", updated.getId(), current.name(), request.status().name());
        operationalMetricsService.recordApplicationStatusTransition(current.name(), request.status().name(), "success");
        operationalEventLogger.emit("application_status_transition", "INFO", actor.userId(), "applications.status.patch", "success", current + "->" + request.status());
        return toResponse(updated);
    }

    @Override
    public ApplicationDraftResponse getById(UUID id) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        ApplicationDraftJpaEntity draft = draftRepository.findByIdAndUserId(id, actor.userId())
                .orElseThrow(() -> new NotFoundException("Candidatura nao encontrada"));
        return toResponse(draft);
    }

    @Override
    public List<ApplicationTrackingEventResponse> listTracking(UUID id) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        ApplicationDraftJpaEntity draft = draftRepository.findByIdAndUserId(id, actor.userId())
                .orElseThrow(() -> new NotFoundException("Candidatura nao encontrada"));
        return trackingRepository.findByApplicationDraftIdOrderByCreatedAtAsc(draft.getId()).stream()
                .map(item -> new ApplicationTrackingEventResponse(
                        item.getId(),
                        resolveTrackingStage(item.getStage()),
                        item.getNotes(),
                        item.getCreatedAt() == null ? OffsetDateTime.now() : item.getCreatedAt()
                ))
                .toList();
    }

    private ApplicationDraftResponse toResponse(ApplicationDraftJpaEntity draft) {
        return new ApplicationDraftResponse(draft.getId(), draft.getVacancyId(), draft.getResumeVariantId(), ApplicationStatus.valueOf(draft.getStatus()), draft.getMessageDraft());
    }

    private TrackingStage toStage(ApplicationStatus status) {
        return switch (status) {
            case DRAFT -> TrackingStage.CREATED;
            case READY_FOR_REVIEW -> TrackingStage.SCREENING;
            case APPLIED -> TrackingStage.SUBMITTED;
            case INTERVIEW -> TrackingStage.INTERVIEW;
            case OFFER -> TrackingStage.FINAL;
            case REJECTED, WITHDRAWN -> TrackingStage.CLOSED;
        };
    }

    private TrackingStage resolveTrackingStage(String persistedStage) {
        if (persistedStage == null || persistedStage.isBlank()) {
            log.warn("eventType=applications.tracking_legacy_stage_fallback reason=blank stageRaw={}", persistedStage);
            return TrackingStage.CREATED;
        }

        String normalized = persistedStage.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CREATED", "SCREENING", "SUBMITTED", "INTERVIEW", "FINAL", "CLOSED" -> TrackingStage.valueOf(normalized);
            case "DRAFT" -> TrackingStage.CREATED;
            case "READY_FOR_REVIEW" -> TrackingStage.SCREENING;
            case "APPLIED" -> TrackingStage.SUBMITTED;
            case "OFFER" -> TrackingStage.FINAL;
            case "REJECTED", "WITHDRAWN" -> TrackingStage.CLOSED;
            default -> {
                log.warn("eventType=applications.tracking_legacy_stage_fallback reason=unknown stageRaw={}", persistedStage);
                yield TrackingStage.CLOSED;
            }
        };
    }
}

