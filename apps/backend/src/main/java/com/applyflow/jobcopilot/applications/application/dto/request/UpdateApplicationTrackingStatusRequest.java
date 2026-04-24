package com.applyflow.jobcopilot.applications.application.dto.request;

import com.applyflow.jobcopilot.applications.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateApplicationTrackingStatusRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 2000) @Pattern(regexp = "^[^\\u0000]*$") String notes
) {
}
