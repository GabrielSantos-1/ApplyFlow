package com.applyflow.jobcopilot.vacancies.infrastructure.persistence;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionRepository;
import com.applyflow.jobcopilot.vacancies.application.service.VacancyIngestionNormalizer;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancyJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VacancyIngestionPersistenceRepositoryTest {

    @Test
    void shouldUseChecksumFallbackForDeduplication() {
        VacancyJpaRepository repository = mock(VacancyJpaRepository.class);
        VacancyIngestionPersistenceRepository ingestionRepository = new VacancyIngestionPersistenceRepository(repository, new ObjectMapper());

        UUID platformId = VacancyIngestionNormalizer.REMOTIVE_PLATFORM_ID;
        NormalizedVacancyRecord record = normalized(platformId, "external-1", "checksum-1");

        VacancyJpaEntity existing = new VacancyJpaEntity();
        existing.setId(UUID.randomUUID());
        existing.setPlatformId(platformId);
        existing.setExternalJobId("another-external");
        existing.setChecksum("checksum-1");
        existing.setTitle(record.title());
        existing.setCanonicalTitle(record.canonicalTitle());
        existing.setCompany(record.companyName());
        existing.setCanonicalCompany(record.canonicalCompanyName());
        existing.setLocation(record.location());
        existing.setCanonicalLocation(record.canonicalLocation());
        existing.setRemote(record.remote());
        existing.setSeniority(record.seniority());
        existing.setNormalizedSeniority(record.normalizedSeniority());
        existing.setQualityScore(record.qualityScore());
        existing.setDedupeKey(record.dedupeKey());
        existing.setRequiredSkills(String.join(",", record.requiredSkills()));
        existing.setSourceUrl(record.sourceUrl());
        existing.setRawDescription(record.descriptionRaw());
        existing.setRemoteType(record.remoteType());
        existing.setEmploymentType(record.employmentType());
        existing.setStatus(VacancyStatus.PUBLISHED.name());

        when(repository.findBySourceAndSourceTenantAndExternalJobId("REMOTIVE", "remotive.com", "external-1")).thenReturn(Optional.empty());
        when(repository.findByPlatformIdAndSourceChecksum(platformId, "checksum-1")).thenReturn(Optional.of(existing));
        when(repository.findFirstByDedupeKeyAndDuplicateRecordFalseOrderByDiscoveredAtAsc(any())).thenReturn(Optional.of(existing));

        VacancyIngestionRepository.UpsertOutcome outcome = ingestionRepository.upsert(record);

        assertEquals(VacancyIngestionRepository.UpsertOutcome.DUPLICATE_UNCHANGED, outcome);
        verify(repository, never()).save(argThat(entity -> entity.getId() == null));
    }

    @Test
    void shouldInsertWhenNoDuplicateExists() {
        VacancyJpaRepository repository = mock(VacancyJpaRepository.class);
        VacancyIngestionPersistenceRepository ingestionRepository = new VacancyIngestionPersistenceRepository(repository, new ObjectMapper());

        UUID platformId = VacancyIngestionNormalizer.REMOTIVE_PLATFORM_ID;
        NormalizedVacancyRecord record = normalized(platformId, "external-2", "checksum-2");

        when(repository.findBySourceAndSourceTenantAndExternalJobId("REMOTIVE", "remotive.com", "external-2")).thenReturn(Optional.empty());
        when(repository.findByPlatformIdAndSourceChecksum(platformId, "checksum-2")).thenReturn(Optional.empty());
        when(repository.findFirstByDedupeKeyAndDuplicateRecordFalseOrderByDiscoveredAtAsc(any())).thenReturn(Optional.empty());

        VacancyIngestionRepository.UpsertOutcome outcome = ingestionRepository.upsert(record);

        assertEquals(VacancyIngestionRepository.UpsertOutcome.INSERTED, outcome);
        verify(repository).save(any(VacancyJpaEntity.class));
    }

    @Test
    void shouldSoftFlagCrossSourceDuplicatesUsingDedupeKey() {
        VacancyJpaRepository repository = mock(VacancyJpaRepository.class);
        VacancyIngestionPersistenceRepository ingestionRepository = new VacancyIngestionPersistenceRepository(repository, new ObjectMapper());

        UUID platformId = VacancyIngestionNormalizer.LEVER_PLATFORM_ID;
        NormalizedVacancyRecord record = new NormalizedVacancyRecord(
                platformId,
                VacancyIngestionSource.LEVER,
                "lever.co",
                "lever-100",
                "https://lever.co/jobs/lever-100",
                "checksum-100",
                "Senior Backend Engineer",
                "senior backend engineer",
                "Google LLC",
                "google",
                "Remote",
                "remote",
                "REMOTE",
                "FULL_TIME",
                true,
                "Senior",
                "senior",
                List.of("Java", "Spring"),
                85,
                List.of("SENIORITY_NORMALIZED"),
                "t:senior backend engineer|c:google|l:remote|r:true|s:senior|k:java,spring",
                "Descricao extensa para vaga",
                "Requisitos extensos para vaga",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                new ObjectMapper().createObjectNode().put("id", "lever-100"),
                new ObjectMapper().createObjectNode().put("id", "lever-100")
        );

        VacancyJpaEntity canonical = new VacancyJpaEntity();
        canonical.setId(UUID.randomUUID());
        canonical.setDedupeKey(record.dedupeKey());
        canonical.setDuplicateRecord(false);

        when(repository.findBySourceAndSourceTenantAndExternalJobId("LEVER", "lever.co", "lever-100")).thenReturn(Optional.empty());
        when(repository.findByPlatformIdAndSourceChecksum(platformId, "checksum-100")).thenReturn(Optional.empty());
        when(repository.findFirstByDedupeKeyAndDuplicateRecordFalseOrderByDiscoveredAtAsc(record.dedupeKey())).thenReturn(Optional.of(canonical));

        VacancyIngestionRepository.UpsertOutcome outcome = ingestionRepository.upsert(record);

        assertEquals(VacancyIngestionRepository.UpsertOutcome.INSERTED, outcome);
        verify(repository).save(argThat(entity -> entity.isDuplicateRecord() && canonical.getId().equals(entity.getCanonicalVacancyId())));
    }

    private NormalizedVacancyRecord normalized(UUID platformId, String externalId, String checksum) {
        ObjectMapper mapper = new ObjectMapper();
        return new NormalizedVacancyRecord(
                platformId,
                VacancyIngestionSource.REMOTIVE,
                "remotive.com",
                externalId,
                "https://remotive.com/jobs/" + externalId,
                checksum,
                "Senior Backend",
                "senior backend",
                "ApplyFlow",
                "applyflow",
                "Remote",
                "remote",
                "REMOTE",
                "FULL_TIME",
                true,
                "senior",
                "senior",
                List.of("Java", "Spring"),
                90,
                List.of(),
                "t:senior backend|c:applyflow|l:remote|r:true|s:senior|k:java,spring",
                "Descricao",
                "Descricao",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                mapper.createObjectNode().put("id", externalId),
                mapper.createObjectNode().put("id", externalId)
        );
    }
}
