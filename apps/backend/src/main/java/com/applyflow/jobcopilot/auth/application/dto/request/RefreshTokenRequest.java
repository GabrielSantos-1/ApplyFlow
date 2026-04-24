package com.applyflow.jobcopilot.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank @Size(min = 10, max = 2048) String refreshToken
) {
}
