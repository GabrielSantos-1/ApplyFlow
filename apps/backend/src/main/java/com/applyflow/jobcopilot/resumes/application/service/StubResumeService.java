package com.applyflow.jobcopilot.resumes.application.service;

import com.applyflow.jobcopilot.resumes.application.dto.request.CreateResumeMetadataRequest;
import com.applyflow.jobcopilot.resumes.application.dto.request.GenerateResumeVariantRequest;
import com.applyflow.jobcopilot.resumes.application.dto.request.UploadResumePdfCommand;
import com.applyflow.jobcopilot.resumes.application.port.ResumeFileStoragePort;
import com.applyflow.jobcopilot.resumes.application.dto.response.ResumeResponse;
import com.applyflow.jobcopilot.resumes.application.dto.response.ResumeVariantResponse;
import com.applyflow.jobcopilot.resumes.application.usecase.ResumeUseCase;
import com.applyflow.jobcopilot.resumes.domain.ResumeStatus;
import com.applyflow.jobcopilot.resumes.domain.ResumeVariantStatus;
import com.applyflow.jobcopilot.resumes.infrastructure.config.ResumeStorageProperties;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.io.IOException;
import java.util.UUID;

@Service
public class StubResumeService implements ResumeUseCase {
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private final ResumeJpaRepository resumeRepository;
    private final ResumeVariantJpaRepository variantRepository;
    private final ResumeFileStoragePort resumeFileStoragePort;
    private final ResumeStorageProperties resumeStorageProperties;
    private final AuthContextService authContextService;
    private final TextSanitizer sanitizer;
    private final AuditEventService auditEventService;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;

    public StubResumeService(ResumeJpaRepository resumeRepository,
                             ResumeVariantJpaRepository variantRepository,
                             ResumeFileStoragePort resumeFileStoragePort,
                             ResumeStorageProperties resumeStorageProperties,
                             AuthContextService authContextService,
                             TextSanitizer sanitizer,
                             AuditEventService auditEventService,
                             OperationalMetricsService operationalMetricsService,
                             OperationalEventLogger operationalEventLogger) {
        this.resumeRepository = resumeRepository;
        this.variantRepository = variantRepository;
        this.resumeFileStoragePort = resumeFileStoragePort;
        this.resumeStorageProperties = resumeStorageProperties;
        this.authContextService = authContextService;
        this.sanitizer = sanitizer;
        this.auditEventService = auditEventService;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
    }

    @Override
    public PageResponse<ResumeResponse> list(int page, int size) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        Page<ResumeJpaEntity> dbPage = resumeRepository.findByUserId(actor.userId(), PageRequest.of(page, size));
        return new PageResponse<>(dbPage.getContent().stream().map(this::toResponse).toList(), dbPage.getNumber(), dbPage.getSize(), dbPage.getTotalElements(), dbPage.getTotalPages());
    }

    @Override
    @Transactional
    public ResumeResponse create(CreateResumeMetadataRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        ResumeJpaEntity entity = new ResumeJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(actor.userId());
        entity.setTitle(sanitizer.sanitizeFreeText(request.title(), 120));
        entity.setSourceFileName(sanitizer.sanitizeFreeText(request.sourceFileName(), 255));
        entity.setStatus(ResumeStatus.ACTIVE.name());
        entity.setContentType(null);
        entity.setFileSizeBytes(null);
        entity.setFileChecksumSha256(null);
        entity.setStoragePath(null);
        entity.setUploadedAt(null);
        setAsBaseResume(actor.userId(), entity);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        ResumeJpaEntity saved = resumeRepository.save(entity);
        auditEventService.log(actor.userId(), "RESUME_CREATED", "resume", saved.getId(), null, saved.getStatus());
        operationalMetricsService.recordResumeUpload("metadata_only", "success");
        operationalEventLogger.emit("resume_created_metadata", "INFO", actor.userId(), "resumes.create", "success", saved.getId().toString());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ResumeResponse uploadPdf(UploadResumePdfCommand command) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        operationalMetricsService.recordResumeUpload("started", "attempt");
        validatePdfPayload(command);

        ResumeJpaEntity entity = new ResumeJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(actor.userId());
        entity.setTitle(sanitizer.sanitizeFreeText(command.title(), 120));
        entity.setSourceFileName(sanitizer.sanitizeFreeText(command.sourceFileName(), 255));
        entity.setStatus(ResumeStatus.ACTIVE.name());
        entity.setContentType("application/pdf");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        setAsBaseResume(actor.userId(), entity);

        ResumeJpaEntity saved = resumeRepository.save(entity);
        try {
            ResumeFileStoragePort.StoredResumeFile stored = resumeFileStoragePort.storePdf(actor.userId(), saved.getId(), command.fileContent());
            saved.setStoragePath(stored.storagePath());
            saved.setFileSizeBytes(stored.fileSizeBytes());
            saved.setFileChecksumSha256(stored.checksumSha256());
            saved.setUploadedAt(OffsetDateTime.now());
            saved.setUpdatedAt(OffsetDateTime.now());
            ResumeJpaEntity uploaded = resumeRepository.save(saved);

            auditEventService.log(actor.userId(), "RESUME_PDF_UPLOADED", "resume", uploaded.getId(), null, uploaded.getStatus());
            operationalMetricsService.recordResumeUpload("pdf", "success");
            operationalEventLogger.emit("resume_pdf_upload", "INFO", actor.userId(), "resumes.upload", "success", uploaded.getId().toString());
            return toResponse(uploaded);
        } catch (IOException ex) {
            operationalMetricsService.recordResumeUpload("pdf", "failure");
            operationalEventLogger.emit("resume_pdf_upload", "ERROR", actor.userId(), "resumes.upload", "failure", "storage_failure");
            throw new BadRequestException("Falha ao armazenar arquivo de curriculo");
        }
    }

    @Override
    public ResumeResponse getById(UUID id) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        ResumeJpaEntity entity = resumeRepository.findByIdAndUserId(id, actor.userId())
                .orElseThrow(() -> new NotFoundException("Curriculo nao encontrado"));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public ResumeVariantResponse createVariant(UUID resumeId, GenerateResumeVariantRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        resumeRepository.findByIdAndUserId(resumeId, actor.userId())
                .orElseThrow(() -> new NotFoundException("Curriculo nao encontrado"));

        ResumeVariantJpaEntity variant = new ResumeVariantJpaEntity();
        variant.setId(UUID.randomUUID());
        variant.setResumeId(resumeId);
        variant.setVacancyId(request.vacancyId());
        variant.setVariantLabel(sanitizer.sanitizeFreeText(request.variantLabel(), 120));
        variant.setStatus(ResumeVariantStatus.DRAFT.name());
        variant.setCreatedAt(OffsetDateTime.now());
        variant.setUpdatedAt(OffsetDateTime.now());
        ResumeVariantJpaEntity saved = variantRepository.save(variant);
        auditEventService.log(actor.userId(), "RESUME_VARIANT_CREATED", "resume_variant", saved.getId(), null, saved.getStatus());
        return new ResumeVariantResponse(saved.getId(), saved.getResumeId(), saved.getVacancyId(), saved.getVariantLabel(), ResumeVariantStatus.valueOf(saved.getStatus()));
    }

    private ResumeResponse toResponse(ResumeJpaEntity entity) {
        return new ResumeResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSourceFileName(),
                ResumeStatus.valueOf(entity.getStatus()),
                entity.isBase(),
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getFileChecksumSha256(),
                entity.getUploadedAt()
        );
    }

    private void setAsBaseResume(UUID userId, ResumeJpaEntity entity) {
        resumeRepository.clearBaseByUserId(userId);
        entity.setBase(true);
    }

    private void validatePdfPayload(UploadResumePdfCommand command) {
        if (command == null || command.fileContent() == null || command.fileContent().length == 0) {
            operationalMetricsService.recordResumeUploadValidationFailure("empty_file");
            throw new BadRequestException("Arquivo PDF obrigatorio");
        }
        if (command.fileContent().length > resumeStorageProperties.getMaxPdfBytes()) {
            operationalMetricsService.recordResumeUploadValidationFailure("file_too_large");
            throw new BadRequestException("Arquivo excede limite permitido");
        }
        if (command.contentType() == null || !"application/pdf".equalsIgnoreCase(command.contentType().trim())) {
            operationalMetricsService.recordResumeUploadValidationFailure("invalid_content_type");
            throw new BadRequestException("Tipo de arquivo invalido; apenas PDF e permitido");
        }
        if (!hasPdfSignature(command.fileContent())) {
            operationalMetricsService.recordResumeUploadValidationFailure("invalid_pdf_signature");
            throw new BadRequestException("Arquivo nao possui assinatura PDF valida");
        }
        if (command.title() == null || command.title().isBlank()) {
            operationalMetricsService.recordResumeUploadValidationFailure("empty_title");
            throw new BadRequestException("Titulo do curriculo e obrigatorio");
        }
        if (command.sourceFileName() == null || command.sourceFileName().isBlank()) {
            operationalMetricsService.recordResumeUploadValidationFailure("empty_filename");
            throw new BadRequestException("Nome do arquivo de origem e obrigatorio");
        }
    }

    private boolean hasPdfSignature(byte[] content) {
        if (content.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (content[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}

