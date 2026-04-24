package com.applyflow.jobcopilot.vacancies.infrastructure.persistence;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConfigurationRepository;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancySourceJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancySourceJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaVacancySourceConfigurationRepository implements VacancySourceConfigurationRepository {
    private final VacancySourceJpaRepository repository;

    public JpaVacancySourceConfigurationRepository(VacancySourceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VacancySourceConfiguration> findEnabled() {
        return repository.findByEnabledTrueOrderByCreatedAtAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<VacancySourceConfiguration> findEnabledById(UUID id) {
        return repository.findByIdAndEnabledTrue(id).map(this::toDomain);
    }

    @Override
    public List<VacancySourceConfiguration> findEnabledBySource(VacancyIngestionSource source) {
        return repository.findByEnabledTrueOrderByCreatedAtAsc().stream()
                .filter(entity -> source.name().equalsIgnoreCase(entity.getSourceType()))
                .map(this::toDomain)
                .toList();
    }

    private VacancySourceConfiguration toDomain(VacancySourceJpaEntity entity) {
        return new VacancySourceConfiguration(
                entity.getId(),
                VacancyIngestionSource.valueOf(entity.getSourceType()),
                entity.getDisplayName(),
                entity.getConfigJson(),
                entity.isEnabled()
        );
    }
}
