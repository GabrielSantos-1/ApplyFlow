package com.applyflow.jobcopilot.matching.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MatchGenerateRequest(
        @NotNull UUID vacancyId,
        UUID resumeId,
        UUID resumeVariantId,
        Boolean forceRegenerate
) {
    public boolean shouldForceRegenerate() {
        return Boolean.TRUE.equals(forceRegenerate);
    }
}
