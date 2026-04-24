package com.applyflow.jobcopilot.matching.domain;

import java.util.Map;
import java.util.Set;

public record MatchingScore(
        int totalScore,
        Map<String, Integer> breakdown,
        Set<String> matchedCoreSkills,
        Set<String> missingMandatorySkills
) {
}
