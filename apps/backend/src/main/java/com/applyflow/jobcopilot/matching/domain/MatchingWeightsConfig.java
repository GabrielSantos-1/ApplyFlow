package com.applyflow.jobcopilot.matching.domain;

public record MatchingWeightsConfig(
        int stack,
        int seniority,
        int locationRemote,
        int mandatoryRequirements,
        int differentiators,
        int textualAdherence
) {
    public static MatchingWeightsConfig defaultV1() {
        return new MatchingWeightsConfig(35, 15, 10, 20, 10, 10);
    }
}
