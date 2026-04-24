package com.applyflow.jobcopilot.matching.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class MatchResult extends BaseEntity {
    private final UUID userId;
    private final UUID vacancyId;
    private final UUID resumeVariantId;
    private final int score;
    private final Map<String, Integer> scoreBreakdown;

    public MatchResult(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID userId, UUID vacancyId,
                       UUID resumeVariantId, int score, Map<String, Integer> scoreBreakdown) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.vacancyId = vacancyId;
        this.resumeVariantId = resumeVariantId;
        this.score = score;
        this.scoreBreakdown = scoreBreakdown;
    }

    public UUID getUserId() { return userId; }
    public UUID getVacancyId() { return vacancyId; }
    public UUID getResumeVariantId() { return resumeVariantId; }
    public int getScore() { return score; }
    public Map<String, Integer> getScoreBreakdown() { return scoreBreakdown; }
}
