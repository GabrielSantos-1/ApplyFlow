package com.applyflow.jobcopilot.ai.application.service;

import com.applyflow.jobcopilot.ai.application.dto.ApplicationDraftSuggestionResponse;
import com.applyflow.jobcopilot.ai.application.dto.CvImprovementResponse;
import com.applyflow.jobcopilot.ai.application.dto.MatchEnrichmentResponse;
import com.applyflow.jobcopilot.ai.application.exception.AiProviderException;
import com.applyflow.jobcopilot.ai.application.port.AiTextGenerationPort.AiTextGenerationResult;
import com.applyflow.jobcopilot.ai.application.port.AiTextGenerationPort;
import com.applyflow.jobcopilot.ai.application.usecase.AiEnrichmentUseCase;
import com.applyflow.jobcopilot.ai.domain.AiFlow;
import com.applyflow.jobcopilot.matching.application.dto.MatchAnalysisResponse;
import com.applyflow.jobcopilot.matching.application.usecase.MatchingUseCase;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.CandidateProfileJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.CandidateProfileJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeVariantJpaRepository;
import com.applyflow.jobcopilot.shared.application.exception.NotFoundException;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiEnrichmentService implements AiEnrichmentUseCase {
    private static final Logger log = LoggerFactory.getLogger(AiEnrichmentService.class);

    private final MatchingUseCase matchingUseCase;
    private final VacancyJpaRepository vacancyRepository;
    private final ResumeJpaRepository resumeRepository;
    private final ResumeVariantJpaRepository variantRepository;
    private final CandidateProfileJpaRepository profileRepository;
    private final AuthContextService authContextService;
    private final AiPromptFactory promptFactory;
    private final AiTextGenerationPort aiProvider;
    private final AiOutputValidator outputValidator;
    private final AiFallbackFactory fallbackFactory;
    private final OperationalMetricsService operationalMetricsService;

    public AiEnrichmentService(MatchingUseCase matchingUseCase,
                               VacancyJpaRepository vacancyRepository,
                               ResumeJpaRepository resumeRepository,
                               ResumeVariantJpaRepository variantRepository,
                               CandidateProfileJpaRepository profileRepository,
                               AuthContextService authContextService,
                               AiPromptFactory promptFactory,
                               AiTextGenerationPort aiProvider,
                               AiOutputValidator outputValidator,
                               AiFallbackFactory fallbackFactory,
                               OperationalMetricsService operationalMetricsService) {
        this.matchingUseCase = matchingUseCase;
        this.vacancyRepository = vacancyRepository;
        this.resumeRepository = resumeRepository;
        this.variantRepository = variantRepository;
        this.profileRepository = profileRepository;
        this.authContextService = authContextService;
        this.promptFactory = promptFactory;
        this.aiProvider = aiProvider;
        this.outputValidator = outputValidator;
        this.fallbackFactory = fallbackFactory;
        this.operationalMetricsService = operationalMetricsService;
    }

    @Override
    @Transactional
    public MatchEnrichmentResponse enrichMatch(UUID vacancyId) {
        AiContext ctx = loadContext(vacancyId);
        AiFlow flow = AiFlow.MATCH_ENRICHMENT;
        long startedNs = System.nanoTime();
        operationalMetricsService.recordAiCallStarted(flow.metricTag(), aiProvider.providerName());
        try {
            AiPromptFactory.PromptEnvelope prompt = promptFactory.build(flow, ctx.vacancy(), ctx.profile(), ctx.resume(), ctx.variant(), ctx.deterministicMatch());
            AiTextGenerationResult generationResult = aiProvider.generate(flow, prompt.systemPrompt(), prompt.userPrompt());
            String raw = generationResult.content();
            AiOutputValidator.ValidatedMatchEnrichment validated = outputValidator.parseMatchEnrichment(raw);
            MatchEnrichmentResponse response = new MatchEnrichmentResponse(
                    vacancyId,
                    ctx.deterministicMatch().score(),
                    ctx.deterministicMatch().recommendation().name(),
                    ctx.deterministicMatch().scoreBreakdown(),
                    validated.summary(),
                    validated.strengths(),
                    validated.gaps(),
                    validated.nextSteps(),
                    false
            );
            recordSuccess(flow, startedNs, generationResult);
            return response;
        } catch (Exception ex) {
            recordFailure(flow, startedNs, classifyOutcome(ex), formatReason(ex));
            return fallbackFactory.fallbackMatchEnrichment(vacancyId, ctx.deterministicMatch());
        }
    }

    @Override
    @Transactional
    public CvImprovementResponse improveCv(UUID vacancyId) {
        AiContext ctx = loadContext(vacancyId);
        AiFlow flow = AiFlow.CV_IMPROVEMENT;
        long startedNs = System.nanoTime();
        operationalMetricsService.recordAiCallStarted(flow.metricTag(), aiProvider.providerName());
        try {
            AiPromptFactory.PromptEnvelope prompt = promptFactory.build(flow, ctx.vacancy(), ctx.profile(), ctx.resume(), ctx.variant(), ctx.deterministicMatch());
            AiTextGenerationResult generationResult = aiProvider.generate(flow, prompt.systemPrompt(), prompt.userPrompt());
            String raw = generationResult.content();
            AiOutputValidator.ValidatedCvImprovement validated = outputValidator.parseCvImprovement(raw);
            CvImprovementResponse response = new CvImprovementResponse(
                    vacancyId,
                    ctx.deterministicMatch().score(),
                    ctx.deterministicMatch().recommendation().name(),
                    ctx.deterministicMatch().scoreBreakdown(),
                    validated.improvementSuggestions(),
                    validated.atsKeywords(),
                    validated.highlightPoints(),
                    validated.gapActions(),
                    false
            );
            recordSuccess(flow, startedNs, generationResult);
            return response;
        } catch (Exception ex) {
            recordFailure(flow, startedNs, classifyOutcome(ex), formatReason(ex));
            return fallbackFactory.fallbackCvImprovement(vacancyId, ctx.deterministicMatch(), ctx.vacancy());
        }
    }

    @Override
    @Transactional
    public ApplicationDraftSuggestionResponse draftApplicationMessage(UUID vacancyId) {
        AiContext ctx = loadContext(vacancyId);
        AiFlow flow = AiFlow.APPLICATION_DRAFT;
        long startedNs = System.nanoTime();
        operationalMetricsService.recordAiCallStarted(flow.metricTag(), aiProvider.providerName());
        try {
            AiPromptFactory.PromptEnvelope prompt = promptFactory.build(flow, ctx.vacancy(), ctx.profile(), ctx.resume(), ctx.variant(), ctx.deterministicMatch());
            AiTextGenerationResult generationResult = aiProvider.generate(flow, prompt.systemPrompt(), prompt.userPrompt());
            String raw = generationResult.content();
            AiOutputValidator.ValidatedApplicationDraft validated = outputValidator.parseApplicationDraft(raw);
            ApplicationDraftSuggestionResponse response = new ApplicationDraftSuggestionResponse(
                    vacancyId,
                    ctx.deterministicMatch().score(),
                    ctx.deterministicMatch().recommendation().name(),
                    ctx.deterministicMatch().scoreBreakdown(),
                    validated.shortMessage(),
                    validated.miniCoverNote(),
                    false
            );
            recordSuccess(flow, startedNs, generationResult);
            return response;
        } catch (Exception ex) {
            recordFailure(flow, startedNs, classifyOutcome(ex), formatReason(ex));
            return fallbackFactory.fallbackApplicationDraft(vacancyId, ctx.deterministicMatch(), ctx.vacancy());
        }
    }

    private AiContext loadContext(UUID vacancyId) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        VacancyJpaEntity vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Vaga nao encontrada"));
        ResumeJpaEntity resume = resumeRepository.findTopByUserIdOrderByCreatedAtDesc(actor.userId())
                .orElseThrow(() -> new NotFoundException("Nenhum curriculo encontrado para o usuario"));
        ResumeVariantJpaEntity variant = variantRepository.findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(resume.getId(), vacancyId)
                .orElseThrow(() -> new NotFoundException("Nenhuma variante do curriculo para a vaga informada"));
        CandidateProfileJpaEntity profile = profileRepository.findByUserId(actor.userId()).orElse(null);
        MatchAnalysisResponse deterministicMatch = matchingUseCase.analyze(vacancyId);
        return new AiContext(vacancy, profile, resume, variant, deterministicMatch);
    }

    private void recordSuccess(AiFlow flow, long startedNs, AiTextGenerationResult result) {
        long durationMs = durationMs(startedNs);
        operationalMetricsService.recordAiCallCompleted(flow.metricTag(), aiProvider.providerName(), "success");
        operationalMetricsService.recordAiCallDuration(flow.metricTag(), aiProvider.providerName(), "success", durationMs);
        Double estimatedCostUsd = estimateCostUsd(result.model(), result.promptTokens(), result.completionTokens());
        log.info("eventType=ai.flow_execution flow={} provider={} outcome=success durationMs={} model={} promptTokens={} completionTokens={} totalTokens={} estimatedCostUsd={}",
                flow.metricTag(),
                aiProvider.providerName(),
                durationMs,
                sanitizeLogValue(result.model(), 60),
                result.promptTokens(),
                result.completionTokens(),
                result.totalTokens(),
                estimatedCostUsd);
    }

    private void recordFailure(AiFlow flow, long startedNs, String outcome, String reason) {
        long durationMs = durationMs(startedNs);
        operationalMetricsService.recordAiCallCompleted(flow.metricTag(), aiProvider.providerName(), outcome);
        operationalMetricsService.recordAiCallDuration(flow.metricTag(), aiProvider.providerName(), outcome, durationMs);
        operationalMetricsService.recordAiFallback(flow.metricTag(), outcome);
        log.warn("eventType=ai.flow_execution flow={} provider={} outcome={} fallback=true durationMs={} reason={}",
                flow.metricTag(), aiProvider.providerName(), outcome, durationMs, reason);
    }

    private long durationMs(long startedNs) {
        return Math.max(1L, (System.nanoTime() - startedNs) / 1_000_000L);
    }

    private String classifyOutcome(Exception ex) {
        if (ex instanceof AiProviderException providerException) {
            String msg = providerException.getMessage() == null ? "" : providerException.getMessage().toLowerCase();
            if (msg.contains("timeout")) {
                return "timeout";
            }
            return "provider_failed";
        }
        if (ex instanceof IllegalArgumentException) {
            return "invalid_output";
        }
        return "failed";
    }

    private String formatReason(Exception ex) {
        String base = ex.getClass().getSimpleName();
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return base;
        }
        return base + ":" + sanitizeLogValue(ex.getMessage(), 140);
    }

    private String sanitizeLogValue(String value, int maxLen) {
        if (value == null) {
            return "null";
        }
        String sanitized = value
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("[^\\p{Print}]", "")
                .trim();
        return sanitized.length() > maxLen ? sanitized.substring(0, maxLen) : sanitized;
    }

    private Double estimateCostUsd(String model, Integer promptTokens, Integer completionTokens) {
        if (model == null || promptTokens == null || completionTokens == null) {
            return null;
        }
        String normalized = model.trim().toLowerCase();
        if (!normalized.startsWith("gpt-4o-mini")) {
            return null;
        }
        double inputCost = (promptTokens / 1_000_000d) * 0.15d;
        double outputCost = (completionTokens / 1_000_000d) * 0.60d;
        return Math.round((inputCost + outputCost) * 1_000_000d) / 1_000_000d;
    }

    private record AiContext(
            VacancyJpaEntity vacancy,
            CandidateProfileJpaEntity profile,
            ResumeJpaEntity resume,
            ResumeVariantJpaEntity variant,
            MatchAnalysisResponse deterministicMatch
    ) {
    }
}
