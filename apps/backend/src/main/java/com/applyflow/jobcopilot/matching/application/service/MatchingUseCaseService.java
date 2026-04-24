package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.matching.application.dto.MatchAnalysisResponse;
import com.applyflow.jobcopilot.matching.application.dto.MatchGenerateRequest;
import com.applyflow.jobcopilot.matching.application.dto.MatchInput;
import com.applyflow.jobcopilot.matching.application.dto.MatchSummaryResponse;
import com.applyflow.jobcopilot.matching.application.usecase.MatchScoringService;
import com.applyflow.jobcopilot.matching.application.usecase.MatchingUseCase;
import com.applyflow.jobcopilot.matching.domain.MatchRecommendation;
import com.applyflow.jobcopilot.matching.domain.MatchState;
import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.matching.domain.MatchingScore;
import com.applyflow.jobcopilot.matching.infrastructure.persistence.entity.MatchResultJpaEntity;
import com.applyflow.jobcopilot.matching.infrastructure.persistence.repository.MatchResultJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.CandidateProfileJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.CandidateProfileJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeVariantJpaRepository;
import com.applyflow.jobcopilot.shared.application.audit.AuditEventService;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.exception.NotFoundException;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchingUseCaseService implements MatchingUseCase {
    private static final Logger log = LoggerFactory.getLogger(MatchingUseCaseService.class);
    private static final String ALGORITHM_VERSION = "deterministic-v1";

    private final MatchScoringService matchScoringService;
    private final MatchingFeatureExtractorService matchingFeatureExtractorService;
    private final MatchingExplanationService matchingExplanationService;
    private final VacancyJpaRepository vacancyRepository;
    private final ResumeJpaRepository resumeRepository;
    private final ResumeVariantJpaRepository variantRepository;
    private final CandidateProfileJpaRepository candidateProfileRepository;
    private final MatchResultJpaRepository matchResultRepository;
    private final AuthContextService authContextService;
    private final AuditEventService auditEventService;
    private final OperationalMetricsService operationalMetricsService;
    private final ObjectMapper objectMapper;

    public MatchingUseCaseService(MatchScoringService matchScoringService,
                                  MatchingFeatureExtractorService matchingFeatureExtractorService,
                                  MatchingExplanationService matchingExplanationService,
                                  VacancyJpaRepository vacancyRepository,
                                  ResumeJpaRepository resumeRepository,
                                  ResumeVariantJpaRepository variantRepository,
                                  CandidateProfileJpaRepository candidateProfileRepository,
                                  MatchResultJpaRepository matchResultRepository,
                                  AuthContextService authContextService,
                                  AuditEventService auditEventService,
                                  OperationalMetricsService operationalMetricsService,
                                  ObjectMapper objectMapper) {
        this.matchScoringService = matchScoringService;
        this.matchingFeatureExtractorService = matchingFeatureExtractorService;
        this.matchingExplanationService = matchingExplanationService;
        this.vacancyRepository = vacancyRepository;
        this.resumeRepository = resumeRepository;
        this.variantRepository = variantRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.matchResultRepository = matchResultRepository;
        this.authContextService = authContextService;
        this.auditEventService = auditEventService;
        this.operationalMetricsService = operationalMetricsService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MatchAnalysisResponse analyze(UUID vacancyId) {
        MatchAnalysisResponse response = generate(new MatchGenerateRequest(vacancyId, null, null, false));
        if (response.state() == MatchState.GENERATED) {
            return response;
        }
        if (response.state() == MatchState.MISSING_RESUME) {
            throw new NotFoundException("Nenhum curriculo encontrado para o usuario");
        }
        if (response.state() == MatchState.MISSING_VARIANT) {
            throw new NotFoundException("Nenhuma variante do curriculo para a vaga informada");
        }
        throw new NotFoundException("Match ainda nao foi gerado para a vaga informada");
    }

    @Override
    @Transactional(readOnly = true)
    public MatchAnalysisResponse getByVacancy(UUID vacancyId) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        ensureVacancyExists(vacancyId);

        MatchResultJpaEntity existing = findLatestResult(actor.userId(), vacancyId);
        if (existing != null) {
            MatchAnalysisResponse safeGenerated = tryBuildGeneratedResponse(existing, true, true);
            if (safeGenerated != null) {
                return safeGenerated;
            }
            log.warn("eventType=matching.read_inconsistent_result actorId={} vacancyId={} matchResultId={} reason=incomplete_persisted_payload",
                    actor.userId(), vacancyId, existing.getId());
        }

        ResumeJpaEntity resume = resolveDefaultResume(actor.userId());
        if (resume == null) {
            operationalMetricsService.recordMatchContextMissing("resume");
            return stateResponse(vacancyId, MatchState.MISSING_RESUME, false, false);
        }

        ResumeVariantJpaEntity variant = variantRepository
                .findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(resume.getId(), vacancyId)
                .orElse(null);
        if (variant == null) {
            operationalMetricsService.recordMatchContextMissing("variant");
            return stateResponse(vacancyId, MatchState.MISSING_VARIANT, true, false);
        }

        operationalMetricsService.recordMatchContextMissing("not_generated");
        return stateResponse(vacancyId, MatchState.NOT_GENERATED, true, true);
    }

    @Override
    @Transactional
    public MatchAnalysisResponse generate(MatchGenerateRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        UUID vacancyId = request.vacancyId();
        VacancyJpaEntity vacancy = ensureVacancyExists(vacancyId);

        ResolvedContext context = resolveContext(actor.userId(), vacancyId, request.resumeId(), request.resumeVariantId());
        if (context.resume() == null) {
            operationalMetricsService.recordMatchContextMissing("resume");
            return stateResponse(vacancyId, MatchState.MISSING_RESUME, false, false);
        }
        if (context.variant() == null) {
            operationalMetricsService.recordMatchContextMissing("variant");
            return stateResponse(vacancyId, MatchState.MISSING_VARIANT, true, false);
        }

        MatchResultJpaEntity existing = findLatestResult(actor.userId(), vacancyId);
        if (existing != null && !request.shouldForceRegenerate()) {
            MatchAnalysisResponse safeGenerated = tryBuildGeneratedResponse(existing, true, true);
            if (safeGenerated != null) {
                return safeGenerated;
            }
            log.warn("eventType=matching.generate_inconsistent_result actorId={} vacancyId={} matchResultId={} forceRegenerate=false reason=will_regenerate",
                    actor.userId(), vacancyId, existing.getId());
        }

        long startedNs = System.nanoTime();
        try {
            MatchInput input = buildInput(vacancyId, context.profile(), context.resume(), context.variant());
            MatchingFeatures features = matchingFeatureExtractorService.extract(vacancy, input);
            MatchingScore score = matchScoringService.calculate(features);
            MatchingExplanationService.MatchExplanation explanation = matchingExplanationService.explain(score, features);

            MatchResultJpaEntity persisted = persistResult(actor.userId(), vacancyId, context.resume().getId(), context.variant().getId(), score, explanation, existing);
            auditEventService.log(actor.userId(), "MATCH_GENERATED", "match_result", persisted.getId(), null, String.valueOf(score.totalScore()));
            operationalMetricsService.recordMatchGenerated("success", explanation.recommendation().name());
            operationalMetricsService.recordMatchGenerationDuration("success", durationMs(startedNs));
            log.info("eventType=matching.generated actorId={} vacancyId={} score={} recommendation={} state={} algorithmVersion={}",
                    actor.userId(), vacancyId, score.totalScore(), explanation.recommendation(), MatchState.GENERATED, ALGORITHM_VERSION);
            return toGeneratedResponse(persisted, true, true);
        } catch (RuntimeException ex) {
            operationalMetricsService.recordMatchGenerated("failed", "unknown");
            operationalMetricsService.recordMatchGenerationDuration("failed", durationMs(startedNs));
            log.warn("eventType=matching.generated actorId={} vacancyId={} outcome=failed algorithmVersion={} reason={}",
                    actor.userId(), vacancyId, ALGORITHM_VERSION, sanitizeReason(ex.getMessage()));
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MatchSummaryResponse getSummary(UUID vacancyId) {
        MatchAnalysisResponse analysis = getByVacancy(vacancyId);
        return new MatchSummaryResponse(
                analysis.vacancyId(),
                analysis.score(),
                analysis.recommendation(),
                analysis.generatedAt(),
                analysis.state()
        );
    }

    private MatchResultJpaEntity persistResult(UUID userId,
                                               UUID vacancyId,
                                               UUID resumeId,
                                               UUID resumeVariantId,
                                               MatchingScore score,
                                               MatchingExplanationService.MatchExplanation explanation,
                                               MatchResultJpaEntity existing) {
        OffsetDateTime now = OffsetDateTime.now();
        MatchResultJpaEntity entity = existing == null ? new MatchResultJpaEntity() : existing;
        if (existing == null) {
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(now);
        }
        entity.setUserId(userId);
        entity.setVacancyId(vacancyId);
        entity.setResumeId(resumeId);
        entity.setResumeVariantId(resumeVariantId);
        entity.setScore((short) score.totalScore());
        entity.setRecommendation(explanation.recommendation().name());
        entity.setScoreBreakdown(objectMapper.valueToTree(score.breakdown()));
        entity.setStrengthsJson(objectMapper.valueToTree(explanation.strengths()));
        entity.setGapsJson(objectMapper.valueToTree(explanation.gaps()));
        entity.setKeywordsToAddJson(objectMapper.valueToTree(explanation.keywordsToAdd()));
        entity.setAlgorithmVersion(ALGORITHM_VERSION);
        entity.setGeneratedAt(now);
        entity.setUpdatedAt(now);
        return matchResultRepository.save(entity);
    }

    private MatchAnalysisResponse toGeneratedResponse(MatchResultJpaEntity entity, boolean hasResumeContext, boolean hasVariantContext) {
        Integer safeScore = entity.getScore() == null ? null : entity.getScore().intValue();
        MatchRecommendation safeRecommendation = parseRecommendation(entity.getRecommendation());
        return new MatchAnalysisResponse(
                entity.getVacancyId(),
                safeScore,
                safeRecommendation,
                toBreakdown(entity.getScoreBreakdown()),
                toStringList(entity.getStrengthsJson()),
                toStringList(entity.getGapsJson()),
                toStringList(entity.getKeywordsToAddJson()),
                MatchState.GENERATED,
                entity.getAlgorithmVersion() == null ? ALGORITHM_VERSION : entity.getAlgorithmVersion(),
                resolveGeneratedAt(entity),
                hasResumeContext,
                hasVariantContext
        );
    }

    private MatchAnalysisResponse tryBuildGeneratedResponse(MatchResultJpaEntity entity, boolean hasResumeContext, boolean hasVariantContext) {
        if (entity == null || entity.getVacancyId() == null) {
            return null;
        }
        if (entity.getScore() == null) {
            return null;
        }
        if (parseRecommendation(entity.getRecommendation()) == null) {
            return null;
        }
        return toGeneratedResponse(entity, hasResumeContext, hasVariantContext);
    }

    private MatchAnalysisResponse stateResponse(UUID vacancyId, MatchState state, boolean hasResumeContext, boolean hasVariantContext) {
        return new MatchAnalysisResponse(
                vacancyId,
                null,
                null,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                state,
                ALGORITHM_VERSION,
                null,
                hasResumeContext,
                hasVariantContext
        );
    }

    private VacancyJpaEntity ensureVacancyExists(UUID vacancyId) {
        return vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Vaga nao encontrada"));
    }

    private ResolvedContext resolveContext(UUID userId,
                                           UUID vacancyId,
                                           UUID requestedResumeId,
                                           UUID requestedVariantId) {
        CandidateProfileJpaEntity profile = candidateProfileRepository.findByUserId(userId).orElse(null);
        if (requestedVariantId != null) {
            ResumeVariantJpaEntity variant = variantRepository.findById(requestedVariantId)
                    .orElseThrow(() -> new NotFoundException("Variante de curriculo nao encontrada"));
            ResumeJpaEntity resume = resumeRepository.findByIdAndUserId(variant.getResumeId(), userId)
                    .orElseThrow(() -> new NotFoundException("Curriculo nao encontrado para o usuario"));
            if (requestedResumeId != null && !requestedResumeId.equals(resume.getId())) {
                throw new BadRequestException("resumeId diverge da variante informada");
            }
            if (variant.getVacancyId() == null || !vacancyId.equals(variant.getVacancyId())) {
                throw new BadRequestException("Variante informada nao pertence a vaga solicitada");
            }
            return new ResolvedContext(profile, resume, variant);
        }

        ResumeJpaEntity resume = resolveResume(userId, requestedResumeId);
        if (resume == null) {
            return new ResolvedContext(profile, null, null);
        }
        ResumeVariantJpaEntity variant = variantRepository.findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(resume.getId(), vacancyId)
                .orElse(null);
        return new ResolvedContext(profile, resume, variant);
    }

    private ResumeJpaEntity resolveDefaultResume(UUID userId) {
        return resumeRepository.findFirstByUserIdAndBaseTrue(userId)
                .or(() -> resumeRepository.findTopByUserIdOrderByCreatedAtDesc(userId))
                .orElse(null);
    }

    private ResumeJpaEntity resolveResume(UUID userId, UUID requestedResumeId) {
        if (requestedResumeId != null) {
            return resumeRepository.findByIdAndUserId(requestedResumeId, userId)
                    .orElseThrow(() -> new NotFoundException("Curriculo informado nao encontrado para o usuario"));
        }
        return resolveDefaultResume(userId);
    }

    private MatchInput buildInput(UUID vacancyId,
                                  CandidateProfileJpaEntity profile,
                                  ResumeJpaEntity resume,
                                  ResumeVariantJpaEntity variant) {
        MatchInput.CandidateProfileData candidateProfileData = new MatchInput.CandidateProfileData(
                profile == null ? null : profile.getHeadline(),
                profile == null ? null : profile.getSummary(),
                profile == null ? null : profile.getLocation(),
                split(profile == null ? null : profile.getPrimarySkills())
        );

        MatchInput.ResumeData resumeData = new MatchInput.ResumeData(
                resume.getId(),
                variant.getId(),
                resume.getTitle(),
                resume.getSourceFileName(),
                variant.getVariantLabel()
        );

        return new MatchInput(vacancyId, candidateProfileData, resumeData);
    }

    private List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private Map<String, Integer> toBreakdown(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<>() {
            });
        } catch (IllegalArgumentException ex) {
            return Map.of();
        }
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.convertValue(node, new TypeReference<>() {
            });
            return values == null ? List.of() : values;
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private MatchRecommendation parseRecommendation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MatchRecommendation.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private OffsetDateTime resolveGeneratedAt(MatchResultJpaEntity entity) {
        if (entity.getGeneratedAt() != null) {
            return entity.getGeneratedAt();
        }
        if (entity.getUpdatedAt() != null) {
            return entity.getUpdatedAt();
        }
        return entity.getCreatedAt();
    }

    private MatchResultJpaEntity findLatestResult(UUID userId, UUID vacancyId) {
        return matchResultRepository.findTopByUserIdAndVacancyIdOrderByCreatedAtDesc(userId, vacancyId).orElse(null);
    }

    private long durationMs(long startedNs) {
        return Math.max(1L, (System.nanoTime() - startedNs) / 1_000_000L);
    }

    private String sanitizeReason(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String sanitized = raw.replaceAll("[\\r\\n\\t]+", " ").replaceAll("[^\\p{Print}]", "").trim();
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    private record ResolvedContext(
            CandidateProfileJpaEntity profile,
            ResumeJpaEntity resume,
            ResumeVariantJpaEntity variant
    ) {
    }
}
