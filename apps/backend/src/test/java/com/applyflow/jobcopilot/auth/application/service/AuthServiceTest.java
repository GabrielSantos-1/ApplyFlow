package com.applyflow.jobcopilot.auth.application.service;

import com.applyflow.jobcopilot.auth.application.dto.request.LoginRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.RefreshTokenRequest;
import com.applyflow.jobcopilot.auth.domain.UserRole;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.entity.UserJpaEntity;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.mapper.UserMapper;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.applyflow.jobcopilot.shared.application.audit.AuditEventService;
import com.applyflow.jobcopilot.shared.application.exception.ForbiddenException;
import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private UserJpaRepository userRepository;
    private RefreshTokenJpaRepository refreshTokenRepository;
    private JwtService jwtService;
    private StubAuthService service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserJpaRepository.class);
        refreshTokenRepository = mock(RefreshTokenJpaRepository.class);
        jwtService = mock(JwtService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder(4);
        service = new StubAuthService(
                userRepository,
                refreshTokenRepository,
                new UserMapper(),
                encoder,
                jwtService,
                mock(AuthContextService.class),
                mock(AuditEventService.class),
                mock(OperationalMetricsService.class),
                mock(OperationalEventLogger.class)
        );
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("jwt");
        when(jwtService.calculateRefreshExpiry()).thenReturn(Instant.now().plusSeconds(3600));
    }

    @Test
    void loginWithValidCredentialShouldReturnTokens() {
        String hash = new BCryptPasswordEncoder(4).encode("Password123!");
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail("user@test.com");
        entity.setPasswordHash(hash);
        entity.setRole(UserRole.USER.name());
        entity.setActive(true);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(entity));

        var result = service.login(new LoginRequest("user@test.com", "Password123!"));

        assertEquals("jwt", result.accessToken());
        assertNotNull(result.refreshToken());
        verify(refreshTokenRepository, atLeastOnce()).save(any());
    }

    @Test
    void loginInvalidPasswordShouldFail() {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail("user@test.com");
        entity.setPasswordHash(new BCryptPasswordEncoder(4).encode("Different123!"));
        entity.setRole(UserRole.USER.name());
        entity.setActive(true);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(entity));

        assertThrows(ForbiddenException.class, () -> service.login(new LoginRequest("user@test.com", "Password123!")));
    }

    @Test
    void refreshRevokedShouldFail() {
        RefreshTokenJpaEntity token = new RefreshTokenJpaEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(UUID.randomUUID());
        token.setTokenHash("hash");
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        assertThrows(ForbiddenException.class, () -> service.refresh(new RefreshTokenRequest("plain-token")));
    }

    @Test
    void refreshValidShouldRotate() {
        UUID userId = UUID.randomUUID();
        RefreshTokenJpaEntity token = new RefreshTokenJpaEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenHash("hash");
        token.setExpiresAt(Instant.now().plusSeconds(120));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        UserJpaEntity user = new UserJpaEntity();
        user.setId(userId);
        user.setEmail("user@test.com");
        user.setPasswordHash("x");
        user.setRole(UserRole.USER.name());
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var refreshed = service.refresh(new RefreshTokenRequest("plain-token"));
        assertNotNull(refreshed.refreshToken());
        verify(refreshTokenRepository, atLeast(2)).save(any());
    }
}
