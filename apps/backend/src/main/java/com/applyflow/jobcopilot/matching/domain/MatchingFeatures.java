package com.applyflow.jobcopilot.matching.domain;

import java.util.Set;

public record MatchingFeatures(
        Set<String> candidateSkills,
        Set<String> stackSkills,
        Set<String> mandatoryRequirements,
        Set<String> differentiators,
        String candidateSeniority,
        String vacancySeniority,
        String candidateLocation,
        String vacancyLocation,
        boolean vacancyRemote,
        String candidateText,
        String vacancyText
) {
}
