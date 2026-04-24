package com.applyflow.jobcopilot.ai.application.usecase;

import com.applyflow.jobcopilot.ai.application.dto.ApplicationDraftSuggestionResponse;
import com.applyflow.jobcopilot.ai.application.dto.CvImprovementResponse;
import com.applyflow.jobcopilot.ai.application.dto.MatchEnrichmentResponse;

import java.util.UUID;

public interface AiEnrichmentUseCase {
    MatchEnrichmentResponse enrichMatch(UUID vacancyId);

    CvImprovementResponse improveCv(UUID vacancyId);

    ApplicationDraftSuggestionResponse draftApplicationMessage(UUID vacancyId);
}

