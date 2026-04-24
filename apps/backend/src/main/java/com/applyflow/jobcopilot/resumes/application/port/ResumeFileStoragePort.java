package com.applyflow.jobcopilot.resumes.application.port;

import java.io.IOException;
import java.util.UUID;

public interface ResumeFileStoragePort {
    StoredResumeFile storePdf(UUID userId, UUID resumeId, byte[] content) throws IOException;

    record StoredResumeFile(String storagePath, long fileSizeBytes, String checksumSha256) {
    }
}
