package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancySourceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancySourceJpaRepository extends JpaRepository<VacancySourceJpaEntity, UUID> {
    List<VacancySourceJpaEntity> findByEnabledTrueOrderByCreatedAtAsc();

    List<VacancySourceJpaEntity> findAllByOrderBySourceTypeAscDisplayNameAsc();

    Optional<VacancySourceJpaEntity> findByIdAndEnabledTrue(UUID id);
}
