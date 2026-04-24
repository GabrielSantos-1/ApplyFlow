package com.applyflow.jobcopilot.vacancies.interfaces.http;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.vacancies.application.dto.request.VacancyIngestionRunRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.request.VacancySearchRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyIngestionRunResponse;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyResponse;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyIngestionUseCase;
import com.applyflow.jobcopilot.vacancies.application.usecase.VacancyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vacancies")
@Validated
public class VacancyController {
    private final VacancyUseCase vacancyUseCase;
    private final VacancyIngestionUseCase vacancyIngestionUseCase;

    public VacancyController(VacancyUseCase vacancyUseCase, VacancyIngestionUseCase vacancyIngestionUseCase) {
        this.vacancyUseCase = vacancyUseCase;
        this.vacancyIngestionUseCase = vacancyIngestionUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<VacancyResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                              @RequestParam(required = false) @Pattern(regexp = "(?i)createdAt|title|company") String sortBy,
                                                              @RequestParam(required = false) @Pattern(regexp = "(?i)asc|desc") String sortDirection,
                                                              @RequestParam(required = false) @Size(max = 100) String query,
                                                              @RequestParam(required = false) @Pattern(regexp = "(?i)remote|hybrid|onsite|all") String workModel,
                                                              @RequestParam(required = false) @Pattern(regexp = "(?i)junior|pleno|senior|especialista|all") String seniority) {
        VacancySearchRequest request = new VacancySearchRequest(page, size, sortBy, sortDirection, query, workModel, seniority);
        return ResponseEntity.ok(vacancyUseCase.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VacancyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyUseCase.getById(id));
    }

    @PostMapping("/ingestion/runs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VacancyIngestionRunResponse> runIngestion(@Valid @RequestBody(required = false) VacancyIngestionRunRequest request) {
        VacancyIngestionSource source = resolveSource(request);
        VacancyIngestionExecutionResult result = vacancyIngestionUseCase.run(source, IngestionTriggerType.MANUAL);
        VacancyIngestionRunResponse response = new VacancyIngestionRunResponse(
                result.runId(),
                result.source().name(),
                result.sourceConfigId(),
                result.triggerType().name(),
                result.status(),
                result.fetchedCount(),
                result.normalizedCount(),
                result.insertedCount(),
                result.updatedCount(),
                result.skippedCount(),
                result.failedCount(),
                null,
                null,
                result.startedAt(),
                result.finishedAt(),
                result.errorSummary()
        );
        return ResponseEntity.ok(response);
    }

    private VacancyIngestionSource resolveSource(VacancyIngestionRunRequest request) {
        if (request == null || request.source() == null || request.source().isBlank()) {
            return VacancyIngestionSource.REMOTIVE;
        }
        try {
            return VacancyIngestionSource.valueOf(request.source().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Fonte de ingestao invalida");
        }
    }
}
