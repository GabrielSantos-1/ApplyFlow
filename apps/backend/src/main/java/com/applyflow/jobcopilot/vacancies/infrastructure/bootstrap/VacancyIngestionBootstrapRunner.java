package com.applyflow.jobcopilot.vacancies.infrastructure.bootstrap;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyIngestionUseCase;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyIngestionRunJpaRepository;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class VacancyIngestionBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(VacancyIngestionBootstrapRunner.class);

    private final IngestionProperties ingestionProperties;
    private final VacancyJpaRepository vacancyRepository;
    private final VacancyIngestionRunJpaRepository runRepository;
    private final VacancyIngestionUseCase ingestionUseCase;
    private final Environment environment;

    public VacancyIngestionBootstrapRunner(IngestionProperties ingestionProperties,
                                           VacancyJpaRepository vacancyRepository,
                                           VacancyIngestionRunJpaRepository runRepository,
                                           VacancyIngestionUseCase ingestionUseCase,
                                           Environment environment) {
        this.ingestionProperties = ingestionProperties;
        this.vacancyRepository = vacancyRepository;
        this.runRepository = runRepository;
        this.ingestionUseCase = ingestionUseCase;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        long vacancies = vacancyRepository.count();
        long runs = runRepository.count();

        if (vacancies == 0 && runs == 0) {
            log.warn("eventType=vacancy.ingestion.healthcheck outcome=empty_bootstrap_required vacancies={} runs={}", vacancies, runs);
        }

        IngestionProperties.Bootstrap bootstrap = ingestionProperties.getBootstrap();
        if (!ingestionProperties.isEnabled() || !bootstrap.isEnabled()) {
            return;
        }
        if (!isAllowedProfile(bootstrap.getAllowedProfiles())) {
            log.warn("eventType=vacancy.ingestion.bootstrap outcome=skipped reason=profile_not_allowed activeProfiles={}",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        if (bootstrap.isOnlyWhenVacanciesEmpty() && vacancies > 0) {
            log.info("eventType=vacancy.ingestion.bootstrap outcome=skipped reason=vacancies_not_empty vacancies={}", vacancies);
            return;
        }
        if (bootstrap.isRequireNoRuns() && runs > 0) {
            log.info("eventType=vacancy.ingestion.bootstrap outcome=skipped reason=runs_already_exist runs={}", runs);
            return;
        }

        VacancyIngestionSource source;
        try {
            source = VacancyIngestionSource.valueOf(bootstrap.normalizedSource());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("ingestion.bootstrap.source invalido: " + bootstrap.getSource());
        }

        try {
            VacancyIngestionExecutionResult result = ingestionUseCase.run(source, IngestionTriggerType.BOOTSTRAP);
            log.info("eventType=vacancy.ingestion.bootstrap outcome={} source={} runId={} fetched={} inserted={} updated={} skipped={} failed={}",
                    result.status(), source.name(), result.runId(), result.fetchedCount(), result.insertedCount(), result.updatedCount(), result.skippedCount(), result.failedCount());
        } catch (Exception ex) {
            log.error("eventType=vacancy.ingestion.bootstrap outcome=failed source={} reason={}", source.name(), ex.toString());
        }
    }

    private boolean isAllowedProfile(java.util.List<String> allowedProfiles) {
        if (allowedProfiles == null || allowedProfiles.isEmpty()) {
            return true;
        }
        Set<String> allowed = allowedProfiles.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> active = Arrays.stream(environment.getActiveProfiles())
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (active.isEmpty() && environment.getDefaultProfiles() != null) {
            active = Arrays.stream(environment.getDefaultProfiles())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
        }
        return active.stream().anyMatch(allowed::contains);
    }
}
