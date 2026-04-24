package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface VacancyJpaRepository extends JpaRepository<VacancyJpaEntity, UUID>, JpaSpecificationExecutor<VacancyJpaEntity> {
    Optional<VacancyJpaEntity> findByPlatformIdAndExternalId(UUID platformId, String externalId);

    Optional<VacancyJpaEntity> findByPlatformIdAndSourceChecksum(UUID platformId, String sourceChecksum);

    Optional<VacancyJpaEntity> findBySourceAndSourceTenantAndExternalJobId(String source, String sourceTenant, String externalJobId);

    Optional<VacancyJpaEntity> findFirstByDedupeKeyAndDuplicateRecordFalseOrderByDiscoveredAtAsc(String dedupeKey);
}
