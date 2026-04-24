package com.applyflow.jobcopilot.applications.application.dto.response;

import com.applyflow.jobcopilot.applications.domain.ApplicationStatus;

import java.util.UUID;

public record ApplicationDraftResponse(
        UUID id,
        UUID vacancyId,
        UUID resumeVariantId,
        ApplicationStatus status,
        String messageDraft
) {
}
