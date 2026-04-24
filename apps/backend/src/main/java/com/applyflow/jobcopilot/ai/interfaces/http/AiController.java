package com.applyflow.jobcopilot.ai.interfaces.http;

import com.applyflow.jobcopilot.ai.application.dto.ApplicationDraftSuggestionResponse;
import com.applyflow.jobcopilot.ai.application.dto.CvImprovementResponse;
import com.applyflow.jobcopilot.ai.application.dto.MatchEnrichmentResponse;
import com.applyflow.jobcopilot.ai.application.usecase.AiEnrichmentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/matches")
@Validated
public class AiController {
    private final AiEnrichmentUseCase aiEnrichmentUseCase;

    public AiController(AiEnrichmentUseCase aiEnrichmentUseCase) {
        this.aiEnrichmentUseCase = aiEnrichmentUseCase;
    }

    @PostMapping("/{vacancyId}/enrichment")
    public ResponseEntity<MatchEnrichmentResponse> enrichMatch(@PathVariable UUID vacancyId) {
        return ResponseEntity.ok(aiEnrichmentUseCase.enrichMatch(vacancyId));
    }

    @PostMapping("/{vacancyId}/cv-improvement")
    public ResponseEntity<CvImprovementResponse> improveCv(@PathVariable UUID vacancyId) {
        return ResponseEntity.ok(aiEnrichmentUseCase.improveCv(vacancyId));
    }

    @PostMapping("/{vacancyId}/application-draft")
    public ResponseEntity<ApplicationDraftSuggestionResponse> draftApplication(@PathVariable UUID vacancyId) {
        return ResponseEntity.ok(aiEnrichmentUseCase.draftApplicationMessage(vacancyId));
    }
}

