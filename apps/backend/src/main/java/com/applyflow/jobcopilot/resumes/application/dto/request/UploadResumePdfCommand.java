package com.applyflow.jobcopilot.resumes.application.dto.request;

public record UploadResumePdfCommand(
        String title,
        String sourceFileName,
        String contentType,
        byte[] fileContent
) {
}
