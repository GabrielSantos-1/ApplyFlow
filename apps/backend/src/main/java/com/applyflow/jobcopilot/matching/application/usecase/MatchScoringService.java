package com.applyflow.jobcopilot.matching.application.usecase;

import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.matching.domain.MatchingScore;

public interface MatchScoringService {
    MatchingScore calculate(MatchingFeatures features);
}
