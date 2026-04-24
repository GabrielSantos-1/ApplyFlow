package com.applyflow.jobcopilot.auth.application.dto.response;

import com.applyflow.jobcopilot.auth.domain.UserRole;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String email, UserRole role) {
}
