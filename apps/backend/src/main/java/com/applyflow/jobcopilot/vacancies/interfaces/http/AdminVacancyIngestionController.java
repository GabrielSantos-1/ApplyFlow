package com.applyflow.jobcopilot.vacancies.interfaces.http;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.vacancies.application.dto.request.AdminVacancyIngestionRunRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.AdminVacancyIngestionBatchResponse;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyIngestionRunResponse;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyIngestionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vacancies/ingestion/runs")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminVacancyIngestionController {
    private final VacancyIngestionUseCase vacancyIngestionUseCase;
    private final AuthContextService authContextService;

    public AdminVacancyIngestionController(VacancyIngestionUseCase vacancyIngestionUseCase,
                                           AuthContextService authContextService) {
        this.vacancyIngestionUseCase = vacancyIngestionUseCase;
        this.authContextService = authContextService;
    }

    @PostMapping
    public ResponseEntity<AdminVacancyIngestionBatchResponse> run(@Valid @RequestBody(required = false) AdminVacancyIngestionRunRequest request,
                                                                   @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId) {
        String correlation = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        return ResponseEntity.ok(vacancyIngestionUseCase.runAdmin(request, authContextService.requireAuthenticatedUser(), correlation));
    }

    @GetMapping
    public ResponseEntity<PageResponse<VacancyIngestionRunResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(vacancyIngestionUseCase.listRuns(page, size));
    }
}
