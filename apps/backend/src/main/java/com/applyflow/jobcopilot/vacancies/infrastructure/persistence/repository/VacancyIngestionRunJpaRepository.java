package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyIngestionRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VacancyIngestionRunJpaRepository extends JpaRepository<VacancyIngestionRunJpaEntity, UUID> {
}
