package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.exception.NotFoundException;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.vacancies.application.dto.request.CreateJobSearchPreferenceRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.request.UpdateJobSearchPreferenceRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.JobSearchPreferenceResponse;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.usecase.JobSearchPreferenceUseCase;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.UserJobSearchPreferenceJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.UserJobSearchPreferenceJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class JobSearchPreferenceService implements JobSearchPreferenceUseCase {
    private static final int MAX_PREFERENCES_PER_USER = 10;
    private static final Set<VacancyIngestionSource> SEARCH_PROVIDERS = Set.of(
            VacancyIngestionSource.REMOTIVE,
            VacancyIngestionSource.ADZUNA
    );
    private static final Set<String> SENIORITIES = Set.of("junior", "pleno", "senior", "especialista");

    private final UserJobSearchPreferenceJpaRepository repository;
    private final AuthContextService authContextService;

    public JobSearchPreferenceService(UserJobSearchPreferenceJpaRepository repository,
                                      AuthContextService authContextService) {
        this.repository = repository;
        this.authContextService = authContextService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobSearchPreferenceResponse> list() {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        return repository.findByUserIdOrderByCreatedAtAsc(actor.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public JobSearchPreferenceResponse create(CreateJobSearchPreferenceRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        if (repository.countByUserId(actor.userId()) >= MAX_PREFERENCES_PER_USER) {
            throw new BadRequestException("Limite de pesquisas por usuario atingido");
        }

        NormalizedText keyword = normalizeRequired(request.keyword(), "keyword", 80);
        OffsetDateTime now = OffsetDateTime.now();
        UserJobSearchPreferenceJpaEntity entity = new UserJobSearchPreferenceJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(actor.userId());
        entity.setKeyword(keyword.display());
        entity.setNormalizedKeyword(keyword.normalized());
        entity.setLocation(normalizeOptional(request.location(), "location", 80));
        entity.setRemoteOnly(Boolean.TRUE.equals(request.remoteOnly()));
        entity.setSeniority(normalizeSeniority(request.seniority()));
        entity.setProvider(normalizeProvider(request.provider()).name());
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            return toResponse(repository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Pesquisa ja cadastrada para este provider");
        }
    }

    @Override
    @Transactional
    public JobSearchPreferenceResponse update(UUID id, UpdateJobSearchPreferenceRequest request) {
        AuthenticatedUser actor = authContextService.requireAuthenticatedUser();
        UserJobSearchPreferenceJpaEntity entity = repository.findByIdAndUserId(id, actor.userId())
                .orElseThrow(() -> new NotFoundException("Pesquisa nao encontrada"));

        if (request.keyword() != null) {
            NormalizedText keyword = normalizeRequired(request.keyword(), "keyword", 80);
            entity.setKeyword(keyword.display());
            entity.setNormalizedKeyword(keyword.normalized());
        }
        if (request.location() != null) {
            entity.setLocation(normalizeOptional(request.location(), "location", 80));
        }
        if (request.remoteOnly() != null) {
            entity.setRemoteOnly(request.remoteOnly());
        }
        if (request.seniority() != null) {
            entity.setSeniority(normalizeSeniority(request.seniority()));
        }
        if (request.provider() != null) {
            entity.setProvider(normalizeProvider(request.provider()).name());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        entity.setUpdatedAt(OffsetDateTime.now());
        try {
            return toResponse(repository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Pesquisa ja cadastrada para este provider");
        }
    }

    private VacancyIngestionSource normalizeProvider(String raw) {
        VacancyIngestionSource source;
        try {
            source = VacancyIngestionSource.fromValue(raw);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Provider invalido");
        }
        if (!SEARCH_PROVIDERS.contains(source)) {
            throw new BadRequestException("Provider nao suportado para pesquisa controlada");
        }
        return source;
    }

    private String normalizeSeniority(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = normalizeRequired(raw, "seniority", 30).normalized();
        if (!SENIORITIES.contains(value)) {
            throw new BadRequestException("Seniority invalida");
        }
        return value;
    }

    private String normalizeOptional(String raw, String field, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return normalizeRequired(raw, field, maxLength).display();
    }

    private NormalizedText normalizeRequired(String raw, String field, int maxLength) {
        if (raw == null) {
            throw new BadRequestException("Campo obrigatorio: " + field);
        }
        String display = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
        if (display.length() < 2 || display.length() > maxLength) {
            throw new BadRequestException("Campo invalido: " + field);
        }
        if (display.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequestException("Campo contem caracteres de controle: " + field);
        }
        if (!display.matches("^[\\p{L}\\p{N} .,+#/-]+$")) {
            throw new BadRequestException("Campo contem caracteres nao permitidos: " + field);
        }
        String noAccents = Normalizer.normalize(display, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String normalized = noAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        return new NormalizedText(display, normalized);
    }

    private JobSearchPreferenceResponse toResponse(UserJobSearchPreferenceJpaEntity entity) {
        return new JobSearchPreferenceResponse(
                entity.getId(),
                entity.getKeyword(),
                entity.getNormalizedKeyword(),
                entity.getLocation(),
                entity.isRemoteOnly(),
                entity.getSeniority(),
                entity.getProvider(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastRunAt(),
                entity.getLastRunStatus(),
                entity.getLastFetchedCount(),
                entity.getLastInsertedCount(),
                entity.getLastUpdatedCount(),
                entity.getLastSkippedCount(),
                entity.getLastFailedCount()
        );
    }

    private record NormalizedText(String display, String normalized) {
    }
}
