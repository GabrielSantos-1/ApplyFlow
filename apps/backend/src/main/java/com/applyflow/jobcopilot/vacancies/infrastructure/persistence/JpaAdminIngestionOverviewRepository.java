package com.applyflow.jobcopilot.vacancies.infrastructure.persistence;

import com.applyflow.jobcopilot.vacancies.application.ingestion.port.AdminIngestionOverviewRepository;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancySourceJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.VacancySourceJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
public class JpaAdminIngestionOverviewRepository implements AdminIngestionOverviewRepository {
    private final VacancySourceJpaRepository sourceRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public JpaAdminIngestionOverviewRepository(VacancySourceJpaRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Override
    public List<SourceOverviewRow> listSources() {
        return sourceRepository.findAllByOrderBySourceTypeAscDisplayNameAsc().stream()
                .map(source -> new SourceOverviewRow(
                        source.getId(),
                        source.getDisplayName(),
                        source.getSourceType(),
                        tenant(source),
                        source.isEnabled()
                ))
                .toList();
    }

    @Override
    public List<RunTotalsRow> aggregateRunTotals() {
        return entityManager.createNativeQuery("""
                        select source_config_id,
                               coalesce(sum(fetched_count), 0),
                               coalesce(sum(inserted_count + updated_count), 0),
                               coalesce(sum(skipped_count), 0),
                               coalesce(sum(failed_count), 0)
                        from vacancy_ingestion_runs
                        where source_config_id is not null
                        group by source_config_id
                        """)
                .getResultList()
                .stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new RunTotalsRow(
                            uuid(values[0]),
                            number(values[1]),
                            number(values[2]),
                            number(values[3]),
                            number(values[4])
                    );
                })
                .toList();
    }

    @Override
    public List<LastRunRow> latestRuns() {
        return entityManager.createNativeQuery("""
                        select distinct on (source_config_id)
                               source_config_id,
                               status,
                               coalesce((extract(epoch from (coalesce(finished_at, started_at) - started_at)) * 1000)::bigint, 0),
                               started_at,
                               finished_at,
                               fetched_count,
                               inserted_count,
                               updated_count,
                               skipped_count,
                               failed_count
                        from vacancy_ingestion_runs
                        where source_config_id is not null
                        order by source_config_id, started_at desc
                        """)
                .getResultList()
                .stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new LastRunRow(
                            uuid(values[0]),
                            text(values[1]),
                            number(values[2]),
                            timestamp(values[3]),
                            timestamp(values[4]),
                            number(values[5]),
                            number(values[6]),
                            number(values[7]),
                            number(values[8]),
                            number(values[9])
                    );
                })
                .toList();
    }

    @Override
    public List<VacancyAggregateRow> aggregateVacancies(OffsetDateTime since24h, OffsetDateTime since7d) {
        return entityManager.createQuery("""
                        select v.source,
                               v.sourceTenant,
                               count(v),
                               sum(case when v.duplicateRecord = true then 1 else 0 end),
                               avg(v.qualityScore),
                               sum(case when v.createdAt >= :since24h then 1 else 0 end),
                               sum(case when v.createdAt >= :since7d then 1 else 0 end)
                        from VacancyJpaEntity v
                        group by v.source, v.sourceTenant
                        """, Object[].class)
                .setParameter("since24h", since24h)
                .setParameter("since7d", since7d)
                .getResultList()
                .stream()
                .map(values -> new VacancyAggregateRow(
                        text(values[0]),
                        text(values[1]),
                        number(values[2]),
                        number(values[3]),
                        decimal(values[4]),
                        number(values[5]),
                        number(values[6])
                ))
                .toList();
    }

    @Override
    public List<QualityFlagRow> topQualityFlags(int limit) {
        return entityManager.createNativeQuery("""
                        select flag, count(*)
                        from vacancies v
                        cross join lateral jsonb_array_elements_text(v.quality_flags) flag
                        group by flag
                        order by count(*) desc, flag asc
                        limit :limit
                        """)
                .setParameter("limit", limit)
                .getResultList()
                .stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new QualityFlagRow(text(values[0]), number(values[1]));
                })
                .toList();
    }

    private String tenant(VacancySourceJpaEntity source) {
        if (source.getConfigJson() != null && source.getConfigJson().hasNonNull("tenant")) {
            String tenant = source.getConfigJson().path("tenant").asText();
            if (!tenant.isBlank()) {
                return tenant;
            }
        }
        return source.getDisplayName();
    }

    private UUID uuid(Object value) {
        if (value instanceof UUID id) {
            return id;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private double decimal(Object value) {
        return value == null ? 0 : ((Number) value).doubleValue();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private OffsetDateTime timestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Tipo temporal inesperado em overview de ingestao");
    }
}
