package com.applyflow.jobcopilot.matching.application.dto;

import com.applyflow.jobcopilot.matching.domain.MatchRecommendation;
import com.applyflow.jobcopilot.matching.domain.MatchState;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchSummaryResponse(
        UUID vacancyId,
        Integer score,
        MatchRecommendation recommendation,
        OffsetDateTime generatedAt,
        MatchState state
) {
}
