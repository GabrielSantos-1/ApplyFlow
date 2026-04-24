package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.shared.application.exception.NotFoundException;
import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.application.dto.request.VacancySearchRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyResponse;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyUseCase;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StubVacancyService implements VacancyUseCase {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "title", "company");

    private final VacancyJpaRepository vacancyRepository;
    private final TextSanitizer sanitizer;

    public StubVacancyService(VacancyJpaRepository vacancyRepository, TextSanitizer sanitizer) {
        this.vacancyRepository = vacancyRepository;
        this.sanitizer = sanitizer;
    }

    @Override
    public PageResponse<VacancyResponse> list(VacancySearchRequest request) {
        Sort sort = Sort.by("createdAt").descending();
        if (request.sortBy() != null && ALLOWED_SORT_FIELDS.contains(request.sortBy())) {
            Sort.Direction dir = "asc".equalsIgnoreCase(request.sortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = "createdAt";
            if ("title".equals(request.sortBy())) sortField = "title";
            if ("company".equals(request.sortBy())) sortField = "company";
            sort = Sort.by(dir, sortField);
        }

        String sanitizedQuery = sanitizer.sanitizeFreeText(request.query(), 100);
        VacancySearchRequest sanitizedRequest = new VacancySearchRequest(
                request.page(),
                request.size(),
                request.sortBy(),
                request.sortDirection(),
                sanitizedQuery,
                request.workModel(),
                request.seniority()
        );

        Pageable pageable = PageRequest.of(sanitizedRequest.page(), sanitizedRequest.size(), sort);
        Page<VacancyJpaEntity> page = vacancyRepository.findAll(spec(sanitizedRequest), pageable);
        List<VacancyResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public VacancyResponse getById(UUID id) {
        VacancyJpaEntity entity = vacancyRepository.findById(id).orElseThrow(() -> new NotFoundException("Vaga nao encontrada"));
        return toResponse(entity);
    }

    private Specification<VacancyJpaEntity> spec(VacancySearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.query() != null && !request.query().isBlank()) {
                String q = "%" + request.query().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), q), cb.like(cb.lower(root.get("company")), q)));
            }
            if (request.seniority() != null && !"all".equalsIgnoreCase(request.seniority())) {
                predicates.add(cb.equal(cb.lower(root.get("seniority")), request.seniority().toLowerCase(Locale.ROOT)));
            }
            if (request.workModel() != null) {
                if ("remote".equalsIgnoreCase(request.workModel())) predicates.add(cb.isTrue(root.get("remote")));
                if ("onsite".equalsIgnoreCase(request.workModel())) predicates.add(cb.isFalse(root.get("remote")));
            }
            predicates.add(cb.isFalse(root.get("duplicateRecord")));
            predicates.add(cb.equal(root.get("status"), VacancyStatus.PUBLISHED.name()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private VacancyResponse toResponse(VacancyJpaEntity entity) {
        List<String> skills = entity.getRequiredSkills() == null || entity.getRequiredSkills().isBlank()
                ? List.of()
                : Arrays.stream(entity.getRequiredSkills().split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        return new VacancyResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getCompany(),
                entity.getLocation(),
                entity.isRemote(),
                entity.getSeniority(),
                entity.getSourceUrl(),
                entity.getPublishedAt(),
                VacancyStatus.valueOf(entity.getStatus()),
                skills
        );
    }
}
