package com.applyflow.jobcopilot.ai.application.dto;

import java.util.Map;
import java.util.UUID;

public record ApplicationDraftSuggestionResponse(
        UUID vacancyId,
        int deterministicScore,
        String deterministicRecommendation,
        Map<String, Integer> deterministicBreakdown,
        String shortMessage,
        String miniCoverNote,
        boolean fallbackUsed
) {
}

