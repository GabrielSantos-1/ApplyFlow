package com.applyflow.jobcopilot.matching.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.matching.infrastructure.persistence.entity.MatchResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchResultJpaRepository extends JpaRepository<MatchResultJpaEntity, UUID> {
    Optional<MatchResultJpaEntity> findByUserIdAndVacancyId(UUID userId, UUID vacancyId);

    Optional<MatchResultJpaEntity> findTopByUserIdAndVacancyIdOrderByCreatedAtDesc(UUID userId, UUID vacancyId);
}
