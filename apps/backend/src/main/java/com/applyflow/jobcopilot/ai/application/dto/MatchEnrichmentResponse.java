package com.applyflow.jobcopilot.ai.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MatchEnrichmentResponse(
        UUID vacancyId,
        int deterministicScore,
        String deterministicRecommendation,
        Map<String, Integer> deterministicBreakdown,
        String summary,
        List<String> strengths,
        List<String> gaps,
        List<String> nextSteps,
        boolean fallbackUsed
) {
}

