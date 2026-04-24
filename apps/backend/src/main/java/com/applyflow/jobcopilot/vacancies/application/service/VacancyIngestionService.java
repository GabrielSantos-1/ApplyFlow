package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.vacancies.application.dto.request.AdminVacancyIngestionRunRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.AdminVacancyIngestionBatchResponse;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyIngestionRunResponse;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionRunView;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionExecutionLock;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionRunRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionMetrics;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConfigurationRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyIngestionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VacancyIngestionService implements VacancyIngestionUseCase {
    private static final Logger log = LoggerFactory.getLogger(VacancyIngestionService.class);

    private final Map<VacancyIngestionSource, VacancySourceConnector> connectors;
    private final VacancyIngestionNormalizer normalizer;
    private final VacancyIngestionRepository ingestionRepository;
    private final VacancySourceConfigurationRepository sourceConfigurationRepository;
    private final IngestionExecutionLock executionLock;
    private final IngestionRunRepository runRepository;
    private final VacancyIngestionMetrics metrics;

    public VacancyIngestionService(List<VacancySourceConnector> connectorList,
                                   VacancyIngestionNormalizer normalizer,
                                   VacancyIngestionRepository ingestionRepository,
                                   VacancySourceConfigurationRepository sourceConfigurationRepository,
                                   IngestionExecutionLock executionLock,
                                   IngestionRunRepository runRepository,
                                   VacancyIngestionMetrics metrics) {
        this.connectors = new EnumMap<>(VacancyIngestionSource.class);
        connectorList.forEach(connector -> this.connectors.put(connector.source(), connector));
        this.normalizer = normalizer;
        this.ingestionRepository = ingestionRepository;
        this.sourceConfigurationRepository = sourceConfigurationRepository;
        this.executionLock = executionLock;
        this.runRepository = runRepository;
        this.metrics = metrics;
    }

    @Override
    public VacancyIngestionExecutionResult run(VacancyIngestionSource source, IngestionTriggerType triggerType) {
        VacancySourceConfiguration config = sourceConfigurationRepository.findEnabledBySource(source).stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Nenhuma source configuration habilitada para " + source.name()));
        return runSingle(config, triggerType, "system", "ingestion-" + UUID.randomUUID(), 0);
    }

    @Override
    public AdminVacancyIngestionBatchResponse runAdmin(AdminVacancyIngestionRunRequest request,
                                                       AuthenticatedUser actor,
                                                       String correlationId) {
        List<VacancySourceConfiguration> targets;
        if (request != null && request.sourceConfigId() != null) {
            targets = sourceConfigurationRepository.findEnabledById(request.sourceConfigId()).stream().toList();
        } else if (request != null && request.sourceType() != null && !request.sourceType().isBlank()) {
            VacancyIngestionSource source = VacancyIngestionSource.fromValue(request.sourceType());
            targets = sourceConfigurationRepository.findEnabledBySource(source);
        } else {
            targets = sourceConfigurationRepository.findEnabled();
        }

        if (targets.isEmpty()) {
            throw new BadRequestException("Nenhuma source configuration habilitada para executar");
        }

        int requestedLimit = request != null && request.limit() != null ? request.limit() : 0;
        List<VacancyIngestionRunResponse> runs = targets.stream()
                .map(cfg -> toResponse(runSingle(cfg, IngestionTriggerType.MANUAL, actor.email(), correlationId, requestedLimit)))
                .toList();

        int successfulRuns = (int) runs.stream()
                .filter(run -> "SUCCESS".equals(run.status()) || "PARTIAL".equals(run.status()))
                .count();

        return new AdminVacancyIngestionBatchResponse(
                runs.size(),
                successfulRuns,
                runs.size() - successfulRuns,
                runs
        );
    }

    @Override
    public PageResponse<VacancyIngestionRunResponse> listRuns(int page, int size) {
        Page<VacancyIngestionRunView> data = runRepository.listRuns(PageRequest.of(page, size, Sort.by("startedAt").descending()));
        List<VacancyIngestionRunResponse> items = data.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(items, data.getNumber(), data.getSize(), data.getTotalElements(), data.getTotalPages());
    }

    private VacancyIngestionExecutionResult runSingle(VacancySourceConfiguration sourceConfiguration,
                                                      IngestionTriggerType triggerType,
                                                      String triggeredBy,
                                                      String correlationId,
                                                      int limit) {
        VacancyIngestionSource source = sourceConfiguration.sourceType();
        OffsetDateTime startedAt = OffsetDateTime.now();
        UUID runId = runRepository.createRun(source, sourceConfiguration.id(), triggerType, triggeredBy, correlationId, startedAt);
        metrics.recordRunStarted(source, triggerType.name());

        boolean lockAcquired = executionLock.tryAcquire(source);
        if (!lockAcquired) {
            OffsetDateTime finishedAt = OffsetDateTime.now();
            metrics.recordStageFailure(source, "lock");
            metrics.recordRunCompleted(source, "SKIPPED_LOCKED", Duration.between(startedAt, finishedAt));
            log.warn("eventType=vacancy.ingestion stage=lock source={} trigger={} runId={} outcome=skipped reason=already_running",
                    source.name(), triggerType.name(), runId);
            return runRepository.finishRun(
                    runId, source, sourceConfiguration.id(), triggerType, "SKIPPED_LOCKED",
                    0, 0, 0, 0, 0, 0,
                    startedAt, finishedAt, "Ingestao ja em execucao para a fonte"
            );
        }

        int fetchedCount = 0;
        int normalizedCount = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        String status = "SUCCESS";
        String errorSummary = null;
        OffsetDateTime finishedAt;

        try {
            VacancySourceConnector connector = connectors.get(source);
            if (connector == null) {
                throw new BadRequestException("Fonte de ingestao nao suportada: " + source.name());
            }

            List<ExternalVacancyRecord> externalRecords = connector.fetch(sourceConfiguration, limit);
            fetchedCount = externalRecords.size();

            for (ExternalVacancyRecord external : externalRecords) {
                NormalizedVacancyRecord normalized;
                try {
                    normalized = normalizer.normalize(external);
                    normalizedCount++;
                } catch (Exception ex) {
                    failedCount++;
                    metrics.recordStageFailure(source, "normalize");
                    log.warn("eventType=vacancy.ingestion stage=normalize source={} runId={} outcome=failed reason={}",
                            source.name(), runId, ex.toString());
                    continue;
                }

                try {
                    VacancyIngestionRepository.UpsertOutcome outcome = ingestionRepository.upsert(normalized);
                    if (outcome == VacancyIngestionRepository.UpsertOutcome.DUPLICATE_UNCHANGED) {
                        skippedCount++;
                        metrics.recordSkipped(source);
                    } else if (outcome == VacancyIngestionRepository.UpsertOutcome.UPDATED) {
                        updatedCount++;
                        metrics.recordUpdated(source);
                    } else {
                        insertedCount++;
                        metrics.recordInserted(source);
                    }
                } catch (Exception ex) {
                    failedCount++;
                    metrics.recordStageFailure(source, "persist");
                    log.warn("eventType=vacancy.ingestion stage=persist source={} runId={} outcome=failed externalJobId={} reason={}",
                            source.name(), runId, normalized.externalJobId(), ex.toString());
                }
            }

            if (failedCount > 0 && (insertedCount > 0 || updatedCount > 0 || skippedCount > 0)) {
                status = "PARTIAL";
                errorSummary = "Falhas parciais na normalizacao/persistencia";
            } else if (failedCount > 0) {
                status = "FAILED";
                errorSummary = "Falha total na normalizacao/persistencia";
            }

            finishedAt = OffsetDateTime.now();
            metrics.recordRunCompleted(source, status, Duration.between(startedAt, finishedAt));
            log.info("eventType=vacancy.ingestion stage=run source={} trigger={} runId={} outcome={} fetched={} normalized={} inserted={} updated={} skipped={} failed={}",
                    source.name(), triggerType.name(), runId, status, fetchedCount, normalizedCount, insertedCount, updatedCount, skippedCount, failedCount);
            return runRepository.finishRun(
                    runId, source, sourceConfiguration.id(), triggerType, status,
                    fetchedCount, normalizedCount, insertedCount, updatedCount, skippedCount, failedCount,
                    startedAt, finishedAt, errorSummary
            );
        } catch (Exception ex) {
            finishedAt = OffsetDateTime.now();
            metrics.recordStageFailure(source, "fetch");
            metrics.recordRunCompleted(source, "FAILED", Duration.between(startedAt, finishedAt));
            log.error("eventType=vacancy.ingestion stage=fetch source={} trigger={} runId={} outcome=failed reason={}",
                    source.name(), triggerType.name(), runId, ex.toString());
            return runRepository.finishRun(
                    runId, source, sourceConfiguration.id(), triggerType, "FAILED",
                    fetchedCount, normalizedCount, insertedCount, updatedCount, skippedCount, failedCount + 1,
                    startedAt, finishedAt, summarize(ex)
            );
        } finally {
            if (lockAcquired) {
                executionLock.release(source);
            }
        }
    }

    private VacancyIngestionRunResponse toResponse(VacancyIngestionExecutionResult run) {
        return new VacancyIngestionRunResponse(
                run.runId(),
                run.source().name(),
                run.sourceConfigId(),
                run.triggerType().name(),
                run.status(),
                run.fetchedCount(),
                run.normalizedCount(),
                run.insertedCount(),
                run.updatedCount(),
                run.skippedCount(),
                run.failedCount(),
                null,
                null,
                run.startedAt(),
                run.finishedAt(),
                run.errorSummary()
        );
    }

    private VacancyIngestionRunResponse toResponse(VacancyIngestionRunView run) {
        return new VacancyIngestionRunResponse(
                run.runId(),
                run.sourceType().name(),
                run.sourceConfigId(),
                run.triggerType().name(),
                run.status(),
                run.fetchedCount(),
                run.normalizedCount(),
                run.insertedCount(),
                run.updatedCount(),
                run.skippedCount(),
                run.failedCount(),
                run.triggeredBy(),
                run.correlationId(),
                run.startedAt(),
                run.finishedAt(),
                run.errorSummary()
        );
    }

    private String summarize(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 380 ? message.substring(0, 380) : message;
    }
}
