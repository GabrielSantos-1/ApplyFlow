package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionRunView;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface IngestionRunRepository {

    UUID createRun(VacancyIngestionSource source,
                   UUID sourceConfigId,
                   IngestionTriggerType triggerType,
                   String triggeredBy,
                   String correlationId,
                   OffsetDateTime startedAt);

    VacancyIngestionExecutionResult finishRun(UUID runId,
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
                                              String errorSummary);

    Page<VacancyIngestionRunView> listRuns(Pageable pageable);
}
