package com.applyflow.jobcopilot.matching.interfaces.http;

import com.applyflow.jobcopilot.matching.application.dto.MatchAnalysisResponse;
import com.applyflow.jobcopilot.matching.application.dto.MatchGenerateRequest;
import com.applyflow.jobcopilot.matching.application.dto.MatchSummaryResponse;
import com.applyflow.jobcopilot.matching.application.usecase.MatchingUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {
    private final MatchingUseCase matchingUseCase;

    public MatchController(MatchingUseCase matchingUseCase) {
        this.matchingUseCase = matchingUseCase;
    }

    @PostMapping
    public ResponseEntity<MatchAnalysisResponse> generate(@Valid @RequestBody MatchGenerateRequest request) {
        return ResponseEntity.ok(matchingUseCase.generate(request));
    }

    @GetMapping("/vacancy/{vacancyId}")
    public ResponseEntity<MatchAnalysisResponse> byVacancy(@PathVariable UUID vacancyId) {
        return ResponseEntity.ok(matchingUseCase.getByVacancy(vacancyId));
    }

    @GetMapping("/vacancy/{vacancyId}/summary")
    public ResponseEntity<MatchSummaryResponse> summary(@PathVariable UUID vacancyId) {
        return ResponseEntity.ok(matchingUseCase.getSummary(vacancyId));
    }

    @GetMapping("/{vacancyId}")
    public ResponseEntity<MatchAnalysisResponse> analyze(@PathVariable UUID vacancyId) {
        return ResponseEntity.ok(matchingUseCase.getByVacancy(vacancyId));
    }
}
