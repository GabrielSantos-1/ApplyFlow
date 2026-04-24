package com.applyflow.jobcopilot.resumes.application.dto.response;

import com.applyflow.jobcopilot.resumes.domain.ResumeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String title,
        String sourceFileName,
        ResumeStatus status,
        boolean base,
        String contentType,
        Long fileSizeBytes,
        String fileChecksumSha256,
        OffsetDateTime uploadedAt
) {
}
