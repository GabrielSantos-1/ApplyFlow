package com.applyflow.jobcopilot.matching.infrastructure.persistence.mapper;

import com.applyflow.jobcopilot.matching.domain.MatchResult;
import com.applyflow.jobcopilot.matching.infrastructure.persistence.entity.MatchResultJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MatchResultMapper {
    public MatchResult toDomain(MatchResultJpaEntity entity) {
        int safeScore = entity.getScore() == null ? 0 : entity.getScore();
        return new MatchResult(entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getUserId(), entity.getVacancyId(), entity.getResumeVariantId(), safeScore, Map.of());
    }
}
