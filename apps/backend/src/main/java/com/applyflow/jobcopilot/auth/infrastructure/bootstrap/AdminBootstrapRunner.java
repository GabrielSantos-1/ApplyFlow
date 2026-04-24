package com.applyflow.jobcopilot.auth.infrastructure.bootstrap;

import com.applyflow.jobcopilot.auth.infrastructure.persistence.entity.UserJpaEntity;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapProperties properties;
    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminBootstrapRunner(AdminBootstrapProperties properties,
                                UserJpaRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                Environment environment) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!isAllowedProfile()) {
            log.warn("eventType=security.bootstrap_admin outcome=skipped reason=profile_not_allowed activeProfiles={}",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        String email = normalize(properties.getEmail());
        String password = properties.getPassword();
        if (email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("bootstrap.admin.email e bootstrap.admin.password sao obrigatorios quando bootstrap.admin.enabled=true");
        }

        OffsetDateTime now = OffsetDateTime.now();
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email).orElseGet(UserJpaEntity::new);

        boolean isNew = user.getId() == null;
        if (isNew) {
            user.setId(UUID.randomUUID());
            user.setCreatedAt(now);
            user.setEmail(email);
        }

        boolean shouldResetPassword = isNew
                || properties.isForcePasswordReset()
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash());
        if (shouldResetPassword) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        user.setRole("ADMIN");
        user.setActive(true);
        user.setUpdatedAt(now);
        try {
            userRepository.save(user);
            log.info("eventType=security.bootstrap_admin outcome={} email={} passwordReset={} userId={}",
                    isNew ? "created" : "updated",
                    maskEmail(email),
                    shouldResetPassword,
                    user.getId());
        } catch (DataIntegrityViolationException ex) {
            UserJpaEntity existing = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> ex);
            existing.setRole("ADMIN");
            existing.setActive(true);
            existing.setUpdatedAt(now);
            if (properties.isForcePasswordReset()) {
                existing.setPasswordHash(passwordEncoder.encode(password));
            }
            userRepository.save(existing);
            log.info("eventType=security.bootstrap_admin outcome=converged email={} passwordReset={} userId={}",
                    maskEmail(email),
                    properties.isForcePasswordReset(),
                    existing.getId());
        }
    }

    private boolean isAllowedProfile() {
        if (properties.getAllowedProfiles() == null || properties.getAllowedProfiles().isEmpty()) {
            return true;
        }
        Set<String> allowed = properties.getAllowedProfiles().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> active = Arrays.stream(environment.getActiveProfiles())
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (active.isEmpty() && environment.getDefaultProfiles() != null) {
            active = Arrays.stream(environment.getDefaultProfiles())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
        }
        return active.stream().anyMatch(allowed::contains);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
