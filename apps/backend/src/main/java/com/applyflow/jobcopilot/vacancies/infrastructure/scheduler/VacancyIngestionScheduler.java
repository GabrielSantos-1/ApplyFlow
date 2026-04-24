package com.applyflow.jobcopilot.vacancies.infrastructure.scheduler;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.service.JobSearchPreferenceIngestionService;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyIngestionUseCase;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VacancyIngestionScheduler {
    private final VacancyIngestionUseCase vacancyIngestionUseCase;
    private final JobSearchPreferenceIngestionService jobSearchPreferenceIngestionService;
    private final IngestionProperties ingestionProperties;

    public VacancyIngestionScheduler(VacancyIngestionUseCase vacancyIngestionUseCase,
                                     JobSearchPreferenceIngestionService jobSearchPreferenceIngestionService,
                                     IngestionProperties ingestionProperties) {
        this.vacancyIngestionUseCase = vacancyIngestionUseCase;
        this.jobSearchPreferenceIngestionService = jobSearchPreferenceIngestionService;
        this.ingestionProperties = ingestionProperties;
    }

    @Scheduled(fixedDelayString = "${ingestion.scheduler.remotive-fixed-delay-ms:1800000}")
    public void runRemotiveIngestion() {
        if (!ingestionProperties.isEnabled()
                || !ingestionProperties.getScheduler().isEnabled()
                || !ingestionProperties.getSources().getRemotive().isEnabled()) {
            return;
        }
        vacancyIngestionUseCase.run(VacancyIngestionSource.REMOTIVE, IngestionTriggerType.SCHEDULED);
    }

    @Scheduled(fixedDelayString = "${ingestion.scheduler.greenhouse-fixed-delay-ms:3600000}")
    public void runGreenhouseIngestion() {
        if (!ingestionProperties.isEnabled()
                || !ingestionProperties.getScheduler().isEnabled()
                || !ingestionProperties.getSources().getGreenhouse().isEnabled()) {
            return;
        }
        vacancyIngestionUseCase.run(VacancyIngestionSource.GREENHOUSE, IngestionTriggerType.SCHEDULED);
    }

    @Scheduled(fixedDelayString = "${ingestion.scheduler.lever-fixed-delay-ms:3600000}")
    public void runLeverIngestion() {
        if (!ingestionProperties.isEnabled()
                || !ingestionProperties.getScheduler().isEnabled()
                || !ingestionProperties.getSources().getLever().isEnabled()) {
            return;
        }
        vacancyIngestionUseCase.run(VacancyIngestionSource.LEVER, IngestionTriggerType.SCHEDULED);
    }

    @Scheduled(fixedDelayString = "${ingestion.scheduler.adzuna-fixed-delay-ms:3600000}")
    public void runAdzunaIngestion() {
        if (!ingestionProperties.isEnabled()
                || !ingestionProperties.getScheduler().isEnabled()
                || !ingestionProperties.getSources().getAdzuna().isEnabled()) {
            return;
        }
        vacancyIngestionUseCase.run(VacancyIngestionSource.ADZUNA, IngestionTriggerType.SCHEDULED);
    }

    @Scheduled(fixedDelayString = "${ingestion.scheduler.job-search-preferences-fixed-delay-ms:3600000}")
    public void runJobSearchPreferenceIngestion() {
        if (!ingestionProperties.isEnabled()
                || !ingestionProperties.getScheduler().isEnabled()) {
            return;
        }
        jobSearchPreferenceIngestionService.runActivePreferences();
    }
}
