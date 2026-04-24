package com.applyflow.jobcopilot.resumes.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateResumeMetadataRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String title,
        @NotBlank @Size(max = 255) @Pattern(regexp = "^[^\\p{Cntrl}]+$") String sourceFileName
) {
}
