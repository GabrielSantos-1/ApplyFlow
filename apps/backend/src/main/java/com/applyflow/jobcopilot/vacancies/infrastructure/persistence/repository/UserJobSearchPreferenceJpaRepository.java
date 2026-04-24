package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.UserJobSearchPreferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJobSearchPreferenceJpaRepository extends JpaRepository<UserJobSearchPreferenceJpaEntity, UUID> {
    List<UserJobSearchPreferenceJpaEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<UserJobSearchPreferenceJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    List<UserJobSearchPreferenceJpaEntity> findByEnabledTrueOrderByUpdatedAtAsc();
}
