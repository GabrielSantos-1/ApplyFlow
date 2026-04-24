package com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.CandidateProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileJpaRepository extends JpaRepository<CandidateProfileJpaEntity, UUID> {
    Optional<CandidateProfileJpaEntity> findByUserId(UUID userId);
}
