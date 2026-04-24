package com.applyflow.jobcopilot.ai.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CvImprovementResponse(
        UUID vacancyId,
        int deterministicScore,
        String deterministicRecommendation,
        Map<String, Integer> deterministicBreakdown,
        List<String> improvementSuggestions,
        List<String> atsKeywords,
        List<String> highlightPoints,
        List<String> gapActions,
        boolean fallbackUsed
) {
}

