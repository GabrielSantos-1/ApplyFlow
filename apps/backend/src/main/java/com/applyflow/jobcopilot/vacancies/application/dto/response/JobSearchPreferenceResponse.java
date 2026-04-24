package com.applyflow.jobcopilot.vacancies.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobSearchPreferenceResponse(
        UUID id,
        String keyword,
        String normalizedKeyword,
        String location,
        boolean remoteOnly,
        String seniority,
        String provider,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastRunAt,
        String lastRunStatus,
        int lastFetchedCount,
        int lastInsertedCount,
        int lastUpdatedCount,
        int lastSkippedCount,
        int lastFailedCount
) {
}
