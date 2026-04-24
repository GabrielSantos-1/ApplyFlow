package com.applyflow.jobcopilot.auth.application.dto.response;

import com.applyflow.jobcopilot.auth.domain.UserRole;

import java.util.UUID;

public record AuthTokensResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        String email,
        UserRole role,
        String refreshToken
) {
}
