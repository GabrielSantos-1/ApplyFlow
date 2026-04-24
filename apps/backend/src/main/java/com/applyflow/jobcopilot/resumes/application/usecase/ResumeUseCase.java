package com.applyflow.jobcopilot.resumes.application.usecase;

import com.applyflow.jobcopilot.resumes.application.dto.request.CreateResumeMetadataRequest;
import com.applyflow.jobcopilot.resumes.application.dto.request.GenerateResumeVariantRequest;
import com.applyflow.jobcopilot.resumes.application.dto.request.UploadResumePdfCommand;
import com.applyflow.jobcopilot.resumes.application.dto.response.ResumeResponse;
import com.applyflow.jobcopilot.resumes.application.dto.response.ResumeVariantResponse;
import com.applyflow.jobcopilot.shared.application.dto.PageResponse;

import java.util.UUID;

public interface ResumeUseCase {
    PageResponse<ResumeResponse> list(int page, int size);
    ResumeResponse create(CreateResumeMetadataRequest request);
    ResumeResponse uploadPdf(UploadResumePdfCommand command);
    ResumeResponse getById(UUID id);
    ResumeVariantResponse createVariant(UUID resumeId, GenerateResumeVariantRequest request);
}
