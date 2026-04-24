package com.applyflow.jobcopilot.resumes.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GenerateResumeVariantRequest(
        @NotNull UUID vacancyId,
        @Size(max = 120) @Pattern(regexp = "^[^\\p{Cntrl}]*$") String variantLabel
) {
}
