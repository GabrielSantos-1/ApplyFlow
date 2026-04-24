package com.applyflow.jobcopilot.applications.application.dto.response;

import com.applyflow.jobcopilot.applications.domain.TrackingStage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationTrackingEventResponse(
        UUID id,
        TrackingStage stage,
        String notes,
        OffsetDateTime createdAt
) {
}

