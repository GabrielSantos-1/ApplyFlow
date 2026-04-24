package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.matching.application.usecase.MatchScoringService;
import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.matching.domain.MatchingScore;
import com.applyflow.jobcopilot.matching.domain.MatchingWeightsConfig;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeterministicMatchScoringService implements MatchScoringService {

    @Override
    public MatchingScore calculate(MatchingFeatures features) {
        MatchingWeightsConfig weights = MatchingWeightsConfig.defaultV1();

        int stack = weightedCoverage(features.candidateSkills(), features.stackSkills(), weights.stack(), weights.stack() / 2);
        int seniority = scoreSeniority(features.candidateSeniority(), features.vacancySeniority(), weights.seniority());
        int location = scoreLocation(features.candidateLocation(), features.vacancyLocation(), features.vacancyRemote(), weights.locationRemote());
        int mandatory = weightedCoverage(features.candidateSkills(), features.mandatoryRequirements(), weights.mandatoryRequirements(), weights.mandatoryRequirements() / 2);
        int differentiators = weightedCoverage(features.candidateSkills(), features.differentiators(), weights.differentiators(), weights.differentiators() / 2);
        int textual = scoreTextual(features.candidateText(), features.vacancyText(), weights.textualAdherence());

        int total = bounded(stack + seniority + location + mandatory + differentiators + textual);
        Map<String, Integer> breakdown = Map.of(
                "stackPrincipal", stack,
                "seniority", seniority,
                "locationRemote", location,
                "mandatoryRequirements", mandatory,
                "differentiators", differentiators,
                "textualAdherence", textual
        );

        Set<String> matchedCore = intersection(features.candidateSkills(), features.stackSkills());
        Set<String> missingMandatory = difference(features.mandatoryRequirements(), features.candidateSkills());
        return new MatchingScore(total, breakdown, matchedCore, missingMandatory);
    }

    private int weightedCoverage(Set<String> candidateSkills, Set<String> targetSkills, int weight, int defaultWhenEmpty) {
        if (targetSkills == null || targetSkills.isEmpty()) {
            return defaultWhenEmpty;
        }
        Set<String> normalizedCandidates = safeSet(candidateSkills);
        long matched = targetSkills.stream().filter(normalizedCandidates::contains).count();
        return bounded((int) Math.round((matched * (double) weight) / targetSkills.size()), 0, weight);
    }

    private int scoreSeniority(String candidate, String vacancy, int weight) {
        Integer candidateLevel = seniorityLevel(candidate);
        Integer vacancyLevel = seniorityLevel(vacancy);
        if (candidateLevel == null || vacancyLevel == null) {
            return 0;
        }
        int diff = candidateLevel - vacancyLevel;
        if (diff == 0) return weight;
        if (diff == 1) return 12;
        if (diff == -1) return 8;
        return diff > 1 ? 9 : 0;
    }

    private int scoreLocation(String candidateLocation, String vacancyLocation, boolean remote, int weight) {
        if (remote) {
            return weight;
        }
        if (candidateLocation == null || candidateLocation.isBlank() || vacancyLocation == null || vacancyLocation.isBlank()) {
            return 0;
        }
        return normalizeToken(candidateLocation).equals(normalizeToken(vacancyLocation)) ? weight : 0;
    }

    private int scoreTextual(String candidateText, String vacancyText, int weight) {
        Set<String> candidateTokens = toTokens(candidateText);
        Set<String> vacancyTokens = toTokens(vacancyText);
        if (candidateTokens.isEmpty() || vacancyTokens.isEmpty()) {
            return 0;
        }
        Set<String> overlap = intersection(candidateTokens, vacancyTokens);
        double ratio = overlap.size() / (double) vacancyTokens.size();
        return bounded((int) Math.round(ratio * weight), 0, weight);
    }

    private Set<String> toTokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9#+.-]+")) {
            if (raw.length() < 3) {
                continue;
            }
            tokens.add(raw);
        }
        return tokens;
    }

    private Integer seniorityLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = normalizeToken(raw);
        if (normalized.contains("junior") || normalized.contains("jr")) return 1;
        if (normalized.contains("pleno") || normalized.contains("mid")) return 2;
        if (normalized.contains("senior") || normalized.contains("sr")) return 3;
        if (normalized.contains("especialista") || normalized.contains("staff") || normalized.contains("principal") || normalized.contains("lead")) return 4;
        return null;
    }

    private Set<String> safeSet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return source;
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return Set.of();
        }
        Set<String> copy = new LinkedHashSet<>(left);
        copy.retainAll(right);
        return copy;
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        if (left == null || left.isEmpty()) {
            return Set.of();
        }
        Set<String> copy = new LinkedHashSet<>(left);
        if (right != null) {
            copy.removeAll(right);
        }
        return copy;
    }

    private int bounded(int raw) {
        return bounded(raw, 0, 100);
    }

    private int bounded(int raw, int min, int max) {
        if (raw < min) return min;
        if (raw > max) return max;
        return raw;
    }

    private String normalizeToken(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
