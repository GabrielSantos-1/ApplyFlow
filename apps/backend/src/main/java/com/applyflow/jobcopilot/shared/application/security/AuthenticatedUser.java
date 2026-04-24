package com.applyflow.jobcopilot.shared.application.security;

import com.applyflow.jobcopilot.auth.domain.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, UserRole role) {
}