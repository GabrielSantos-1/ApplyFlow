package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.matching.domain.MatchingScore;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicMatchScoringServiceTest {
    private final DeterministicMatchScoringService service = new DeterministicMatchScoringService();

    @Test
    void shouldBeDeterministicForSameInput() {
        MatchingFeatures input = completeFeatures();
        MatchingScore first = service.calculate(input);
        MatchingScore second = service.calculate(input);
        assertEquals(first, second);
        assertTrue(first.totalScore() >= 0 && first.totalScore() <= 100);
        assertEquals(6, first.breakdown().size());
    }

    @Test
    void shouldReachHighScoreWhenEverythingMatches() {
        MatchingFeatures input = new MatchingFeatures(
                Set.of("java", "spring", "postgresql", "docker", "kubernetes", "aws"),
                Set.of("java", "spring", "postgresql"),
                Set.of("java", "spring", "postgresql"),
                Set.of("docker", "aws"),
                "senior",
                "senior",
                "sao paulo",
                "sao paulo",
                false,
                "java spring postgresql docker aws",
                "java spring postgresql docker aws"
        );
        MatchingScore score = service.calculate(input);
        assertEquals(100, score.totalScore());
    }

    @Test
    void shouldReachLowScoreWhenThereIsNoMatch() {
        MatchingFeatures input = new MatchingFeatures(
                Set.of("excel"),
                Set.of("java", "spring", "postgresql"),
                Set.of("java", "spring", "postgresql"),
                Set.of("docker", "aws"),
                "junior",
                "senior",
                "porto alegre",
                "sao paulo",
                false,
                "excel office administrativo",
                "java spring postgresql docker aws"
        );
        MatchingScore score = service.calculate(input);
        assertEquals(0, score.totalScore());
        assertTrue(score.missingMandatorySkills().contains("java"));
    }

    @Test
    void shouldHandleIncompleteDataWithoutCrashing() {
        MatchingFeatures input = new MatchingFeatures(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                "unknown",
                "unknown",
                null,
                null,
                false,
                null,
                null
        );
        MatchingScore score = service.calculate(input);
        assertTrue(score.totalScore() >= 0 && score.totalScore() <= 100);
        assertNotNull(score.breakdown());
    }

    @Test
    void shouldBoundToZeroForWorstCase() {
        MatchingFeatures input = new MatchingFeatures(
                Set.of("word"),
                Set.of("java", "spring"),
                Set.of("java", "postgresql"),
                Set.of("docker"),
                "junior",
                "senior",
                "porto alegre",
                "sao paulo",
                false,
                "word administrativo",
                "java spring postgresql docker"
        );
        MatchingScore score = service.calculate(input);
        assertEquals(0, score.totalScore());
    }

    @Test
    void shouldBoundToHundredForPerfectCase() {
        MatchingFeatures input = new MatchingFeatures(
                Set.of("java", "spring", "postgresql", "docker", "aws"),
                Set.of("java", "spring", "postgresql"),
                Set.of("java", "spring", "postgresql"),
                Set.of("docker", "aws"),
                "senior",
                "senior",
                "sao paulo",
                "sao paulo",
                false,
                "java spring postgresql docker aws",
                "java spring postgresql docker aws"
        );
        MatchingScore score = service.calculate(input);
        assertEquals(100, score.totalScore());
    }

    private MatchingFeatures completeFeatures() {
        return new MatchingFeatures(
                Set.of("java", "spring", "postgresql", "docker"),
                Set.of("java", "spring", "postgresql"),
                Set.of("java", "spring", "postgresql"),
                Set.of("docker", "kafka"),
                "senior",
                "senior",
                "sao paulo",
                "sao paulo",
                false,
                "java spring postgresql docker kafka",
                "java spring postgresql docker kafka"
        );
    }
}
