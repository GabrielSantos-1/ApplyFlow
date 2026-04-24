package com.applyflow.jobcopilot.auth.application.service;

import com.applyflow.jobcopilot.auth.application.dto.request.LoginRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.LogoutRequest;
import com.applyflow.jobcopilot.auth.application.dto.request.RefreshTokenRequest;
import com.applyflow.jobcopilot.auth.application.dto.response.AuthTokensResponse;
import com.applyflow.jobcopilot.auth.application.dto.response.CurrentUserResponse;
import com.applyflow.jobcopilot.auth.application.usecase.AuthUseCase;
import com.applyflow.jobcopilot.auth.domain.User;
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
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.shared.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class StubAuthService implements AuthUseCase {
    private final UserJpaRepository userRepository;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthContextService authContextService;
    private final AuditEventService auditEventService;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;

    public StubAuthService(UserJpaRepository userRepository,
                           RefreshTokenJpaRepository refreshTokenRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthContextService authContextService,
                           AuditEventService auditEventService,
                           OperationalMetricsService operationalMetricsService,
                           OperationalEventLogger operationalEventLogger) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authContextService = authContextService;
        this.auditEventService = auditEventService;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
    }

    @Override
    @Transactional
    public AuthTokensResponse login(LoginRequest request) {
        Optional<UserJpaEntity> maybe = userRepository.findByEmailIgnoreCase(request.email());
        if (maybe.isEmpty()) {
            auditEventService.log(null, "LOGIN_FAILED", "auth", null, null, "email-not-found");
            operationalMetricsService.recordAuthLogin("failed");
            operationalEventLogger.emit("auth_login", "WARN", null, "auth.login", "failed", "email-not-found");
            throw new ForbiddenException("Credenciais invalidas");
        }

        User user = userMapper.toDomain(maybe.get());
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditEventService.log(user.getId(), "LOGIN_FAILED", "auth", user.getId(), null, "invalid-password");
            operationalMetricsService.recordAuthLogin("failed");
            operationalEventLogger.emit("auth_login", "WARN", user.getId(), "auth.login", "failed", "invalid-password");
            throw new ForbiddenException("Credenciais invalidas");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshTokenPlain = UUID.randomUUID() + "." + UUID.randomUUID();
        persistRefreshToken(user.getId(), refreshTokenPlain, null);

        auditEventService.log(user.getId(), "LOGIN_SUCCESS", "auth", user.getId(), null, "ok");
        operationalMetricsService.recordAuthLogin("success");
        operationalEventLogger.emit("auth_login", "INFO", user.getId(), "auth.login", "success", "ok");
        return new AuthTokensResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds(), user.getId(), user.getEmail(), user.getRole(), refreshTokenPlain);
    }

    @Override
    @Transactional
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            operationalMetricsService.recordAuthRefresh("missing");
            operationalEventLogger.emit("auth_refresh", "WARN", null, "auth.refresh", "failed", "missing-token");
            throw new ForbiddenException("Credenciais invalidas");
        }
        String hash = sha256(request.refreshToken());
        RefreshTokenJpaEntity stored = refreshTokenRepository.findByTokenHash(hash).orElse(null);
        if (stored == null) {
            operationalMetricsService.recordAuthRefresh("not-found-or-replay");
            operationalEventLogger.emit("auth_refresh", "WARN", null, "auth.refresh", "failed", "not-found-or-replay");
            throw new ForbiddenException("Credenciais invalidas");
        }

        if (stored.getRevokedAt() != null) {
            operationalMetricsService.recordAuthRefresh("revoked");
            operationalEventLogger.emit("auth_refresh", "WARN", stored.getUserId(), "auth.refresh", "failed", "revoked");
            throw new ForbiddenException("Credenciais invalidas");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            operationalMetricsService.recordAuthRefresh("expired");
            operationalEventLogger.emit("auth_refresh", "WARN", stored.getUserId(), "auth.refresh", "failed", "expired");
            throw new ForbiddenException("Credenciais invalidas");
        }

        UserJpaEntity userEntity = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ForbiddenException("Credenciais invalidas"));
        User user = userMapper.toDomain(userEntity);

        String newRefreshPlain = UUID.randomUUID() + "." + UUID.randomUUID();
        String newRefreshHash = sha256(newRefreshPlain);
        stored.setRevokedAt(Instant.now());
        stored.setReplacedByTokenHash(newRefreshHash);
        refreshTokenRepository.save(stored);
        persistRefreshToken(user.getId(), newRefreshPlain, null);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        auditEventService.log(user.getId(), "TOKEN_REFRESH", "auth", user.getId(), null, "rotated");
        operationalMetricsService.recordAuthRefresh("success");
        operationalEventLogger.emit("auth_refresh", "INFO", user.getId(), "auth.refresh", "success", "rotated");
        return new AuthTokensResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds(), user.getId(), user.getEmail(), user.getRole(), newRefreshPlain);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        String hash = sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            auditEventService.log(token.getUserId(), "LOGOUT", "auth", token.getUserId(), null, "refresh-revoked");
        });
    }

    @Override
    public CurrentUserResponse me() {
        AuthenticatedUser user = authContextService.requireAuthenticatedUser();
        return new CurrentUserResponse(user.userId(), user.email(), user.role());
    }

    public String issueRefreshTokenForUser(String email) {
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        String plain = UUID.randomUUID() + "." + UUID.randomUUID();
        persistRefreshToken(user.getId(), plain, null);
        return plain;
    }

    private void persistRefreshToken(UUID userId, String plainToken, Instant forcedExpiry) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTokenHash(sha256(plainToken));
        entity.setExpiresAt(forcedExpiry != null ? forcedExpiry : jwtService.calculateRefreshExpiry());
        entity.setCreatedAt(OffsetDateTime.now());
        refreshTokenRepository.save(entity);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("hash error", ex);
        }
    }
}
