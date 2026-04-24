package com.applyflow.jobcopilot.matching.application.service;

import com.applyflow.jobcopilot.matching.application.dto.MatchInput;
import com.applyflow.jobcopilot.matching.domain.MatchingFeatures;
import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatchingFeatureExtractorService {
    private static final Set<String> DIFFERENTIATOR_KEYWORDS = Set.of(
            "docker", "kubernetes", "k8s", "aws", "azure", "gcp",
            "microservices", "kafka", "redis", "terraform", "ci", "cd",
            "observability", "monitoring", "testing", "testes", "rabbitmq"
    );

    private final TextSanitizer sanitizer;

    public MatchingFeatureExtractorService(TextSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    public MatchingFeatures extract(VacancyJpaEntity vacancy, MatchInput input) {
        Set<String> vacancySkills = normalizeSkillSet(splitCsv(vacancy.getRequiredSkills()));
        Set<String> stackSkills = new LinkedHashSet<>(vacancySkills.stream().limit(5).toList());
        Set<String> mandatoryRequirements = vacancySkills;

        String vacancyText = sanitized(join(
                vacancy.getTitle(),
                vacancy.getCompany(),
                vacancy.getRawDescription(),
                vacancy.getRequiredSkills(),
                vacancy.getSeniority()
        ), 12000);

        String candidateText = sanitized(join(
                input.candidateProfile().headline(),
                input.candidateProfile().summary(),
                input.resumeData().title(),
                input.resumeData().variantLabel(),
                input.resumeData().sourceFileName()
        ), 6000);

        Set<String> candidateSkills = normalizeSkillSet(mergeSkills(
                input.candidateProfile().primarySkills(),
                input.resumeData().title(),
                input.resumeData().variantLabel()
        ));

        Set<String> differentiators = findDifferentiators(vacancyText, mandatoryRequirements);
        String candidateSeniority = inferSeniority(join(input.candidateProfile().headline(), input.candidateProfile().summary(), input.resumeData().title()));
        String vacancySeniority = inferSeniority(join(vacancy.getSeniority(), vacancy.getTitle(), vacancy.getRawDescription()));
        String candidateLocation = sanitized(input.candidateProfile().location(), 120);
        String vacancyLocation = sanitized(vacancy.getLocation(), 120);

        return new MatchingFeatures(
                candidateSkills,
                stackSkills,
                mandatoryRequirements,
                differentiators,
                candidateSeniority,
                vacancySeniority,
                candidateLocation,
                vacancyLocation,
                vacancy.isRemote(),
                candidateText,
                vacancyText
        );
    }

    private List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<String> mergeSkills(List<String> profileSkills, String resumeTitle, String variantLabel) {
        List<String> values = new ArrayList<>();
        if (profileSkills != null) {
            values.addAll(profileSkills);
        }
        values.addAll(extractTokens(resumeTitle));
        values.addAll(extractTokens(variantLabel));
        return values;
    }

    private Set<String> normalizeSkillSet(List<String> rawSkills) {
        if (rawSkills == null || rawSkills.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawSkills) {
            String cleaned = sanitized(raw, 50).toLowerCase(Locale.ROOT).trim();
            if (!cleaned.isBlank()) {
                normalized.add(cleaned);
            }
        }
        return normalized;
    }

    private Set<String> findDifferentiators(String vacancyText, Set<String> mandatoryRequirements) {
        Set<String> found = new LinkedHashSet<>();
        String lower = vacancyText == null ? "" : vacancyText.toLowerCase(Locale.ROOT);
        for (String keyword : DIFFERENTIATOR_KEYWORDS) {
            if (lower.contains(keyword) && !mandatoryRequirements.contains(keyword)) {
                found.add(keyword);
            }
        }
        return found;
    }

    private List<String> extractTokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9#+.-]+")) {
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String inferSeniority(String text) {
        String raw = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (raw.contains("junior") || raw.contains(" jr ")) return "junior";
        if (raw.contains("pleno") || raw.contains("mid")) return "pleno";
        if (raw.contains("senior") || raw.contains(" sr ")) return "senior";
        if (raw.contains("especialista") || raw.contains("staff") || raw.contains("principal") || raw.contains("lead")) return "especialista";
        return "unknown";
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(part);
            }
        }
        return sb.toString();
    }

    private String sanitized(String value, int maxLen) {
        return sanitizer.sanitizeFreeText(value, maxLen);
    }
}
