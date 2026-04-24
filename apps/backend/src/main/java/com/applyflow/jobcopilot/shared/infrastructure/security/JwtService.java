package com.applyflow.jobcopilot.shared.infrastructure.security;

import com.applyflow.jobcopilot.auth.domain.UserRole;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {
    private static final long ACCESS_TOKEN_TTL_SECONDS = 900;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final SecurityProperties properties;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, SecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public String generateAccessToken(UUID userId, String email, UserRole role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .audience(java.util.List.of(properties.getJwt().getAudience()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS))
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("role", role.name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String role = jwt.getClaimAsString("role");
        return new AuthenticatedUser(UUID.fromString(sub), email, UserRole.valueOf(role));
    }

    public long getAccessTokenTtlSeconds() {
        return ACCESS_TOKEN_TTL_SECONDS;
    }

    public Instant calculateRefreshExpiry() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }
}