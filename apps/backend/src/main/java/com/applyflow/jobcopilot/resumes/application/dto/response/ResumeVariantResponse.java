package com.applyflow.jobcopilot.resumes.application.dto.response;

import com.applyflow.jobcopilot.resumes.domain.ResumeVariantStatus;

import java.util.UUID;

public record ResumeVariantResponse(
        UUID id,
        UUID resumeId,
        UUID vacancyId,
        String variantLabel,
        ResumeVariantStatus status
) {
}
