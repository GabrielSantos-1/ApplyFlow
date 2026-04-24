package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancySourceConfigurationRepository {
    List<VacancySourceConfiguration> findEnabled();

    Optional<VacancySourceConfiguration> findEnabledById(UUID id);

    List<VacancySourceConfiguration> findEnabledBySource(VacancyIngestionSource source);
}
