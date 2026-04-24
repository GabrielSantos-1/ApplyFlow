package com.applyflow.jobcopilot.vacancies.infrastructure.persistence;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionRunView;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionRunRepository;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyIngestionRunJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyIngestionRunJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class JpaIngestionRunRepository implements IngestionRunRepository {
    private final VacancyIngestionRunJpaRepository repository;

    public JpaIngestionRunRepository(VacancyIngestionRunJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UUID createRun(VacancyIngestionSource source,
                          UUID sourceConfigId,
                          IngestionTriggerType triggerType,
                          String triggeredBy,
                          String correlationId,
                          OffsetDateTime startedAt) {
        VacancyIngestionRunJpaEntity entity = new VacancyIngestionRunJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setSource(source.name());
        entity.setSourceType(source.name());
        entity.setSourceConfigId(sourceConfigId);
        entity.setTriggerType(triggerType.name());
        entity.setTriggeredBy(triggeredBy);
        entity.setCorrelationId(correlationId);
        entity.setStatus("RUNNING");
        entity.setStartedAt(startedAt);
        entity.setCreatedAt(startedAt);
        entity.setUpdatedAt(startedAt);
        repository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public VacancyIngestionExecutionResult finishRun(UUID runId,
                                                     VacancyIngestionSource source,
                                                     UUID sourceConfigId,
                                                     IngestionTriggerType triggerType,
                                                     String status,
                                                     int fetchedCount,
                                                     int normalizedCount,
                                                     int insertedCount,
                                                     int updatedCount,
                                                     int skippedCount,
                                                     int failedCount,
                                                     OffsetDateTime startedAt,
                                                     OffsetDateTime finishedAt,
                                                     String errorSummary) {
        VacancyIngestionRunJpaEntity entity = repository.findById(runId).orElseGet(VacancyIngestionRunJpaEntity::new);
        entity.setId(runId);
        entity.setSource(source.name());
        entity.setSourceType(source.name());
        entity.setSourceConfigId(sourceConfigId);
        entity.setTriggerType(triggerType.name());
        entity.setStatus(status);
        entity.setFetchedCount(fetchedCount);
        entity.setNormalizedCount(normalizedCount);
        entity.setDuplicateCount(skippedCount);
        entity.setPersistedCount(insertedCount + updatedCount);
        entity.setInsertedCount(insertedCount);
        entity.setUpdatedCount(updatedCount);
        entity.setSkippedCount(skippedCount);
        entity.setFailedCount(failedCount);
        entity.setStartedAt(startedAt);
        entity.setFinishedAt(finishedAt);
        entity.setErrorSummary(errorSummary);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(startedAt);
        }
        entity.setUpdatedAt(finishedAt);
        repository.save(entity);

        return new VacancyIngestionExecutionResult(
                entity.getId(),
                source,
                sourceConfigId,
                triggerType,
                status,
                fetchedCount,
                normalizedCount,
                insertedCount,
                updatedCount,
                skippedCount,
                failedCount,
                startedAt,
                finishedAt,
                errorSummary
        );
    }

    @Override
    public Page<VacancyIngestionRunView> listRuns(Pageable pageable) {
        return repository.findAll(pageable).map(entity -> new VacancyIngestionRunView(
                entity.getId(),
                VacancyIngestionSource.valueOf(entity.getSourceType()),
                entity.getSourceConfigId(),
                IngestionTriggerType.valueOf(entity.getTriggerType()),
                entity.getStatus(),
                entity.getFetchedCount(),
                entity.getNormalizedCount(),
                entity.getInsertedCount(),
                entity.getUpdatedCount(),
                entity.getSkippedCount(),
                entity.getFailedCount(),
                entity.getTriggeredBy(),
                entity.getCorrelationId(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getErrorSummary()
        ));
    }
}
