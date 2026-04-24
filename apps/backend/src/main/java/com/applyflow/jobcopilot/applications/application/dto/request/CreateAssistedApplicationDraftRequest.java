package com.applyflow.jobcopilot.applications.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAssistedApplicationDraftRequest(
        @NotNull UUID vacancyId,
        UUID resumeId,
        @Size(max = 4000) @Pattern(regexp = "^[^\\u0000]*$") String messageDraft
) {
}
