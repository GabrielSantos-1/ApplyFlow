package com.applyflow.jobcopilot.vacancies.interfaces.http;

import com.applyflow.jobcopilot.vacancies.application.dto.response.AdminIngestionOverviewResponse;
import com.applyflow.jobcopilot.vacancies.application.usecase.AdminIngestionOverviewUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ingestion/overview")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIngestionOverviewController {
    private final AdminIngestionOverviewUseCase useCase;

    public AdminIngestionOverviewController(AdminIngestionOverviewUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<AdminIngestionOverviewResponse> overview() {
        return ResponseEntity.ok(useCase.overview());
    }
}
