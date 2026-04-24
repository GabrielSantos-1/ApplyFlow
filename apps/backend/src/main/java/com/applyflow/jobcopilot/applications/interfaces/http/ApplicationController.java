package com.applyflow.jobcopilot.applications.interfaces.http;

import com.applyflow.jobcopilot.applications.application.dto.request.CreateAssistedApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.CreateApplicationDraftRequest;
import com.applyflow.jobcopilot.applications.application.dto.request.UpdateApplicationTrackingStatusRequest;
import com.applyflow.jobcopilot.applications.application.dto.response.ApplicationDraftResponse;
import com.applyflow.jobcopilot.applications.application.dto.response.ApplicationTrackingEventResponse;
import com.applyflow.jobcopilot.applications.application.usecase.ApplicationUseCase;
import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@Validated
public class ApplicationController {
    private final ApplicationUseCase applicationUseCase;

    public ApplicationController(ApplicationUseCase applicationUseCase) {
        this.applicationUseCase = applicationUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ApplicationDraftResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(applicationUseCase.list(page, size));
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApplicationDraftResponse> createDraft(@Valid @RequestBody CreateApplicationDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationUseCase.createDraft(request));
    }

    @PostMapping("/drafts/assisted")
    public ResponseEntity<ApplicationDraftResponse> createDraftAssisted(@Valid @RequestBody CreateAssistedApplicationDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationUseCase.createDraftAssisted(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationDraftResponse> updateStatus(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateApplicationTrackingStatusRequest request) {
        return ResponseEntity.ok(applicationUseCase.updateStatus(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDraftResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationUseCase.getById(id));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<List<ApplicationTrackingEventResponse>> listTracking(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationUseCase.listTracking(id));
    }
}
