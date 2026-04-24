package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.matching.domain.MatchRecommendation;
import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.matching.domain.MatchingScore;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingExplanationServiceTest {
    private final MatchingExplanationService service = new MatchingExplanationService();

    @Test
    void shouldRecommendApplyForHighScoreAndShowStrengths() {
        MatchingScore score = new MatchingScore(
                90,
                Map.of(
                        "stackPrincipal", 30,
                        "mandatoryRequirements", 18,
                        "textualAdherence", 9,
                        "seniority", 15,
                        "locationRemote", 10,
                        "differentiators", 8
                ),
                Set.of("java", "spring"),
                Set.of()
        );

        MatchingFeatures features = minimalFeatures();
        MatchingExplanationService.MatchExplanation explanation = service.explain(score, features);

        assertEquals(MatchRecommendation.APPLY, explanation.recommendation());
        assertTrue(explanation.strengths().stream().anyMatch(s -> s.contains("Stack principal aderente")));
        assertTrue(explanation.gaps().isEmpty());
    }

    @Test
    void shouldExposeGapsAndRecommendIgnoreForLowScore() {
        MatchingScore score = new MatchingScore(
                20,
                Map.of(
                        "stackPrincipal", 2,
                        "mandatoryRequirements", 3,
                        "textualAdherence", 1,
                        "seniority", 2,
                        "locationRemote", 10,
                        "differentiators", 2
                ),
                Set.of("java"),
                Set.of("postgresql", "spring")
        );

        MatchingFeatures features = minimalFeatures();
        MatchingExplanationService.MatchExplanation explanation = service.explain(score, features);

        assertEquals(MatchRecommendation.IGNORE, explanation.recommendation());
        assertTrue(explanation.gaps().stream().anyMatch(g -> g.contains("Requisitos obrigatorios ausentes")));
        assertTrue(explanation.gaps().stream().anyMatch(g -> g.toLowerCase().contains("senioridade")));
    }

    private MatchingFeatures minimalFeatures() {
        return new MatchingFeatures(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                "unknown",
                "unknown",
                "",
                "",
                true,
                "",
                ""
        );
    }
}
