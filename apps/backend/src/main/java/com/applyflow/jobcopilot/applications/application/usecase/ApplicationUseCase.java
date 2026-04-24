package com.applyflow.jobcopilot.applications.application.usecase;

import com.applyflow.jobcopilot.applications.application.dto.request.CreateAssistedApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.CreateApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.UpdateApplicationTrackingStatusRequest;
import com.applyflow.jobcopilot.applications.application.dto.response.ApplicationDraftResponse;
import com.applyflow.jobcopilot.applications.application.dto.response.ApplicationTrackingEventResponse;
import com.applyflow.jobcopilot.shared.application.dto.PageResponse;

import java.util.List;
import java.util.UUID;

public interface ApplicationUseCase {
    PageResponse<ApplicationDraftResponse> list(int page, int size);
    ApplicationDraftResponse createDraft(CreateApplicationDraftRequest request);
    ApplicationDraftResponse createDraftAssisted(CreateAssistedApplicationDraftRequest request);
    ApplicationDraftResponse updateStatus(UUID id, UpdateApplicationTrackingStatusRequest request);
    ApplicationDraftResponse getById(UUID id);
    List<ApplicationTrackingEventResponse> listTracking(UUID id);
}
