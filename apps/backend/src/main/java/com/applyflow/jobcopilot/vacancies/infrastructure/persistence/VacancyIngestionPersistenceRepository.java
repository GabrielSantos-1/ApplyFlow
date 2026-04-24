package com.applyflow.jobcopilot.vacancies.infrastructure.persistence;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionRepository;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class VacancyIngestionPersistenceRepository implements VacancyIngestionRepository {
    private final VacancyJpaRepository vacancyJpaRepository;
    private final ObjectMapper objectMapper;

    public VacancyIngestionPersistenceRepository(VacancyJpaRepository vacancyJpaRepository, ObjectMapper objectMapper) {
        this.vacancyJpaRepository = vacancyJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UpsertOutcome upsert(NormalizedVacancyRecord record) {
        Optional<VacancyJpaEntity> existing = vacancyJpaRepository.findBySourceAndSourceTenantAndExternalJobId(
                record.source().name(), record.sourceTenant(), record.externalJobId()
        );
        if (existing.isEmpty() && record.checksum() != null && !record.checksum().isBlank()) {
            existing = vacancyJpaRepository.findByPlatformIdAndSourceChecksum(record.platformId(), record.checksum());
        }

        if (existing.isEmpty()) {
            VacancyJpaEntity inserted = new VacancyJpaEntity();
            inserted.setId(UUID.randomUUID());
            apply(record, inserted);
            attachCanonicalReference(record, inserted);
            OffsetDateTime now = OffsetDateTime.now();
            inserted.setCreatedAt(now);
            inserted.setUpdatedAt(now);
            inserted.setLastIngestedAt(now);
            inserted.setDiscoveredAt(record.discoveredAt() == null ? now : record.discoveredAt());
            inserted.setStatus(VacancyStatus.PUBLISHED.name());
            vacancyJpaRepository.save(inserted);
            return UpsertOutcome.INSERTED;
        }

        VacancyJpaEntity current = existing.get();
        attachCanonicalReference(record, current);
        if (isUnchanged(record, current)) {
            current.setLastIngestedAt(OffsetDateTime.now());
            current.setUpdatedAt(OffsetDateTime.now());
            vacancyJpaRepository.save(current);
            return UpsertOutcome.DUPLICATE_UNCHANGED;
        }

        apply(record, current);
        current.setUpdatedAt(OffsetDateTime.now());
        current.setLastIngestedAt(OffsetDateTime.now());
        vacancyJpaRepository.save(current);
        return UpsertOutcome.UPDATED;
    }

    private void apply(NormalizedVacancyRecord record, VacancyJpaEntity target) {
        target.setPlatformId(record.platformId());
        target.setSource(record.source().name());
        target.setSourceTenant(record.sourceTenant());
        target.setExternalJobId(record.externalJobId());
        target.setExternalId(record.externalJobId());
        target.setSourceUrl(record.sourceUrl());
        target.setChecksum(record.checksum());
        target.setSourceChecksum(record.checksum());
        target.setTitle(record.title());
        target.setCanonicalTitle(record.canonicalTitle());
        target.setCompany(record.companyName());
        target.setCanonicalCompany(record.canonicalCompanyName());
        target.setLocation(record.location());
        target.setCanonicalLocation(record.canonicalLocation());
        target.setRemoteType(record.remoteType());
        target.setEmploymentType(record.employmentType());
        target.setRemote(record.remote());
        target.setSeniority(record.seniority());
        target.setNormalizedSeniority(record.normalizedSeniority());
        target.setQualityScore(record.qualityScore());
        target.setQualityFlags(objectMapper.valueToTree(record.qualityFlags()));
        target.setDedupeKey(record.dedupeKey());
        target.setRequiredSkills(String.join(",", record.requiredSkills()));
        target.setRawDescription(record.descriptionRaw());
        target.setRequirements(record.requirements());
        target.setNormalizedPayload(record.normalizedPayload());
        target.setRawPayload(record.rawPayload());
        target.setPublishedAt(record.publishedAt());
        if (target.getDiscoveredAt() == null && record.discoveredAt() != null) {
            target.setDiscoveredAt(record.discoveredAt());
        }
        if (target.getStatus() == null || target.getStatus().isBlank()) {
            target.setStatus(VacancyStatus.PUBLISHED.name());
        }
    }

    private boolean isUnchanged(NormalizedVacancyRecord record, VacancyJpaEntity existing) {
        return equalsNullable(record.title(), existing.getTitle())
                && equalsNullable(record.companyName(), existing.getCompany())
                && equalsNullable(record.location(), existing.getLocation())
                && record.remote() == existing.isRemote()
                && equalsNullable(record.seniority(), existing.getSeniority())
                && equalsNullable(record.normalizedSeniority(), existing.getNormalizedSeniority())
                && equalsNullable(String.join(",", record.requiredSkills()), existing.getRequiredSkills())
                && equalsNullable(record.checksum(), existing.getChecksum())
                && record.qualityScore() == existing.getQualityScore()
                && equalsNullable(record.dedupeKey(), existing.getDedupeKey())
                && equalsNullable(record.sourceUrl(), existing.getSourceUrl())
                && equalsNullable(record.descriptionRaw(), existing.getRawDescription())
                && equalsNullable(record.remoteType(), existing.getRemoteType())
                && equalsNullable(record.employmentType(), existing.getEmploymentType());
    }

    private void attachCanonicalReference(NormalizedVacancyRecord record, VacancyJpaEntity target) {
        if (record.dedupeKey() == null || record.dedupeKey().isBlank()) {
            target.setDuplicateRecord(false);
            target.setCanonicalVacancyId(null);
            return;
        }

        Optional<VacancyJpaEntity> canonical = vacancyJpaRepository.findFirstByDedupeKeyAndDuplicateRecordFalseOrderByDiscoveredAtAsc(record.dedupeKey());
        if (canonical.isPresent() && !canonical.get().getId().equals(target.getId())) {
            target.setDuplicateRecord(true);
            target.setCanonicalVacancyId(canonical.get().getId());
            return;
        }

        target.setDuplicateRecord(false);
        target.setCanonicalVacancyId(null);
    }

    private boolean equalsNullable(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
