package com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ResumeJpaRepository extends JpaRepository<ResumeJpaEntity, UUID> {
    Page<ResumeJpaEntity> findByUserId(UUID userId, Pageable pageable);
    java.util.Optional<ResumeJpaEntity> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ResumeJpaEntity> findFirstByUserIdAndBaseTrue(UUID userId);
    Optional<ResumeJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update ResumeJpaEntity r set r.base = false where r.userId = :userId")
    int clearBaseByUserId(UUID userId);
}
