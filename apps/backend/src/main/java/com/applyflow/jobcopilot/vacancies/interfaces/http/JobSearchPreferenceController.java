package com.applyflow.jobcopilot.vacancies.interfaces.http;

import com.applyflow.jobcopilot.vacancies.application.dto.request.CreateJobSearchPreferenceRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.request.UpdateJobSearchPreferenceRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.JobSearchPreferenceResponse;
import com.applyflow.jobcopilot.vacancies.application.usecase.JobSearchPreferenceUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-search-preferences")
@Validated
public class JobSearchPreferenceController {
    private final JobSearchPreferenceUseCase useCase;

    public JobSearchPreferenceController(JobSearchPreferenceUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<List<JobSearchPreferenceResponse>> list() {
        return ResponseEntity.ok(useCase.list());
    }

    @PostMapping
    public ResponseEntity<JobSearchPreferenceResponse> create(@Valid @RequestBody CreateJobSearchPreferenceRequest request) {
        JobSearchPreferenceResponse response = useCase.create(request);
        return ResponseEntity.created(URI.create("/api/v1/job-search-preferences/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JobSearchPreferenceResponse> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateJobSearchPreferenceRequest request) {
        return ResponseEntity.ok(useCase.update(id, request));
    }
}
