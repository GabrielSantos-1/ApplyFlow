package com.applyflow.jobcopilot.ai.application.service;

import com.applyflow.jobcopilot.ai.application.dto.ApplicationDraftSuggestionResponse;
import com.applyflow.jobcopilot.ai.application.dto.CvImprovementResponse;
import com.applyflow.jobcopilot.ai.application.dto.MatchEnrichmentResponse;
import com.applyflow.jobcopilot.matching.application.dto.MatchAnalysisResponse;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AiFallbackFactory {

    public MatchEnrichmentResponse fallbackMatchEnrichment(UUID vacancyId, MatchAnalysisResponse deterministic) {
        List<String> nextSteps = new ArrayList<>();
        if (!deterministic.gaps().isEmpty()) {
            nextSteps.add("Priorize cobrir as lacunas obrigatorias antes de aplicar.");
        }
        if (deterministic.score() < 50) {
            nextSteps.add("Revise o curriculo para alinhar stack e senioridade com a vaga.");
        } else {
            nextSteps.add("Adapte o curriculo com foco nas tecnologias mais exigidas.");
        }
        return new MatchEnrichmentResponse(
                vacancyId,
                deterministic.score(),
                deterministic.recommendation().name(),
                deterministic.scoreBreakdown(),
                "Resumo gerado em modo degradado com base no motor deterministico.",
                deterministic.strengths(),
                deterministic.gaps(),
                nextSteps,
                true
        );
    }

    public CvImprovementResponse fallbackCvImprovement(UUID vacancyId, MatchAnalysisResponse deterministic, VacancyJpaEntity vacancy) {
        List<String> improvements = List.of(
                "Ajuste o titulo do curriculo para refletir a vaga alvo.",
                "Inclua resultados mensuraveis para experiencias alinhadas com " + safe(vacancy.getTitle(), "a vaga") + "."
        );
        List<String> keywords = extractKeywords(vacancy.getRequiredSkills());
        List<String> highlights = deterministic.strengths().isEmpty() ? List.of("Destaque experiencia prática com stack principal.") : deterministic.strengths();
        List<String> gapActions = deterministic.gaps().isEmpty() ? List.of("Reforce evidencias de senioridade e escopo de entregas.") : deterministic.gaps();
        return new CvImprovementResponse(
                vacancyId,
                deterministic.score(),
                deterministic.recommendation().name(),
                deterministic.scoreBreakdown(),
                improvements,
                keywords,
                highlights,
                gapActions,
                true
        );
    }

    public ApplicationDraftSuggestionResponse fallbackApplicationDraft(UUID vacancyId, MatchAnalysisResponse deterministic, VacancyJpaEntity vacancy) {
        String role = safe(vacancy.getTitle(), "a vaga");
        String shortMessage = "Tenho interesse na oportunidade de " + role + " e acredito contribuir com entregas consistentes alinhadas aos requisitos.";
        String miniCover = "Baseei esta mensagem no resultado deterministico de matching. Posso aprofundar experiencias praticas em stack, senioridade e requisitos obrigatorios da vaga.";
        return new ApplicationDraftSuggestionResponse(
                vacancyId,
                deterministic.score(),
                deterministic.recommendation().name(),
                deterministic.scoreBreakdown(),
                shortMessage,
                miniCover,
                true
        );
    }

    private List<String> extractKeywords(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of("java", "spring", "postgresql");
        }
        return List.of(csv.split(",")).stream()
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> v.toLowerCase())
                .distinct()
                .limit(10)
                .toList();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
