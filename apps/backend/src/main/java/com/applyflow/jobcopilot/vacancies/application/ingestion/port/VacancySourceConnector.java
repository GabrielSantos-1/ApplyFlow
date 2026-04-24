package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.JobSearchCriteria;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;

import java.util.List;

public interface VacancySourceConnector {
    VacancyIngestionSource source();

    List<ExternalVacancyRecord> fetch(VacancySourceConfiguration configuration, int requestedLimit);

    default List<ExternalVacancyRecord> fetchSearch(VacancySourceConfiguration configuration,
                                                    JobSearchCriteria criteria,
                                                    int requestedLimit) {
        return fetch(configuration, requestedLimit);
    }
}
