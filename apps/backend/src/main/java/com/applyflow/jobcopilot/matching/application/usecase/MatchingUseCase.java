package com.applyflow.jobcopilot.matching.application.usecase;

import com.applyflow.jobcopilot.matching.application.dto.MatchAnalysisResponse;
import com.applyflow.jobcopilot.matching.application.dto.MatchGenerateRequest;
import com.applyflow.jobcopilot.matching.application.dto.MatchSummaryResponse;

import java.util.UUID;

public interface MatchingUseCase {
    MatchAnalysisResponse analyze(UUID vacancyId);

    MatchAnalysisResponse getByVacancy(UUID vacancyId);

    MatchAnalysisResponse generate(MatchGenerateRequest request);

    MatchSummaryResponse getSummary(UUID vacancyId);
}
