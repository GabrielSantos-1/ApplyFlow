package com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeVariantJpaRepository extends JpaRepository<ResumeVariantJpaEntity, UUID> {
    Optional<ResumeVariantJpaEntity> findByIdAndResumeId(UUID id, UUID resumeId);
    Optional<ResumeVariantJpaEntity> findTopByResumeIdAndVacancyIdOrderByCreatedAtDesc(UUID resumeId, UUID vacancyId);
}
