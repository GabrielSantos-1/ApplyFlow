package com.applyflow.jobcopilot.matching.application.dto;

import com.applyflow.jobcopilot.matching.domain.MatchRecommendation;
import com.applyflow.jobcopilot.matching.domain.MatchState;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public record MatchAnalysisResponse(
        UUID vacancyId,
        Integer score,
        MatchRecommendation recommendation,
        Map<String, Integer> scoreBreakdown,
        List<String> strengths,
        List<String> gaps,
        List<String> keywordsToAdd,
        MatchState state,
        String algorithmVersion,
        OffsetDateTime generatedAt,
        boolean hasResumeContext,
        boolean hasVariantContext
) {
}
