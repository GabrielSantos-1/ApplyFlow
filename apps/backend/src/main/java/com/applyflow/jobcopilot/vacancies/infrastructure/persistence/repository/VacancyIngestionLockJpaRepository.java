package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyIngestionLockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VacancyIngestionLockJpaRepository extends JpaRepository<VacancyIngestionLockJpaEntity, String> {
    @Modifying
    @Query(value = "insert into vacancy_ingestion_locks(source, locked_at) values (:source, now())", nativeQuery = true)
    int insertLock(@Param("source") String source);
}
