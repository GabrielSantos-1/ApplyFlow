package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.matching.domain.MatchRecommendation;
import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.matching.domain.MatchingScore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchingExplanationService {

    public MatchExplanation explain(MatchingScore score, MatchingFeatures features) {
        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        List<String> keywordsToAdd = new ArrayList<>();

        int stack = score.breakdown().getOrDefault("stackPrincipal", 0);
        int mandatory = score.breakdown().getOrDefault("mandatoryRequirements", 0);
        int textual = score.breakdown().getOrDefault("textualAdherence", 0);
        int seniority = score.breakdown().getOrDefault("seniority", 0);

        if (stack >= 24) {
            strengths.add("Stack principal aderente: " + String.join(", ", score.matchedCoreSkills()));
        } else {
            gaps.add("Aderencia parcial de stack principal");
        }

        if (mandatory >= 14) {
            strengths.add("Boa cobertura de requisitos obrigatorios");
        } else if (!score.missingMandatorySkills().isEmpty()) {
            gaps.add("Requisitos obrigatorios ausentes: " + String.join(", ", score.missingMandatorySkills()));
            keywordsToAdd.addAll(score.missingMandatorySkills());
        } else {
            gaps.add("Cobertura insuficiente de requisitos obrigatorios");
        }

        if (textual >= 7) {
            strengths.add("Aderencia textual consistente com a descricao da vaga");
        } else {
            gaps.add("Aderencia textual baixa entre curriculo e vaga");
        }

        if (seniority < 8) {
            gaps.add("Nivel de senioridade desalinhado com a vaga");
        } else {
            strengths.add("Senioridade compativel");
        }

        features.differentiators().stream()
                .filter(diff -> !features.candidateSkills().contains(diff))
                .forEach(keywordsToAdd::add);

        MatchRecommendation recommendation;
        if (score.totalScore() >= 75) {
            recommendation = MatchRecommendation.APPLY;
        } else if (score.totalScore() >= 50) {
            recommendation = MatchRecommendation.REVIEW;
        } else {
            recommendation = MatchRecommendation.IGNORE;
        }

        return new MatchExplanation(
                strengths,
                gaps,
                keywordsToAdd.stream().distinct().toList(),
                recommendation
        );
    }

    public record MatchExplanation(
            List<String> strengths,
            List<String> gaps,
            List<String> keywordsToAdd,
            MatchRecommendation recommendation
    ) {
    }
}
