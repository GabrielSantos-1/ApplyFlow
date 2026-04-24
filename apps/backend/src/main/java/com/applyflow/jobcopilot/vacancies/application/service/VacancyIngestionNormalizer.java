package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class VacancyIngestionNormalizer {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern HTML_ESCAPED_TAG = Pattern.compile("&lt;[^&]*&gt;", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPANY_SUFFIX = Pattern.compile(
            "\\b(incorporated|inc\\.?|llc|ltd\\.?|limited|corp\\.?|corporation|s\\.a\\.?|sa|gmbh|plc)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public static final UUID REMOTIVE_PLATFORM_ID = UUID.fromString("3f940286-62f6-4f86-b858-68fec7702642");
    public static final UUID GREENHOUSE_PLATFORM_ID = UUID.fromString("d5c03468-e26a-4446-b72b-f050f9f4021f");
    public static final UUID LEVER_PLATFORM_ID = UUID.fromString("5a836ad4-0ca0-468b-bf9f-4cf9f47b5f10");
    public static final UUID ADZUNA_PLATFORM_ID = UUID.fromString("8f853181-4db8-43cd-b741-5d5dc4f52f9a");

    private final TextSanitizer sanitizer;
    private final ObjectMapper objectMapper;

    public VacancyIngestionNormalizer(TextSanitizer sanitizer, ObjectMapper objectMapper) {
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
    }

    public NormalizedVacancyRecord normalize(ExternalVacancyRecord raw) {
        String title = sanitizer.sanitizeFreeText(raw.title(), 180);
        String company = sanitizer.sanitizeFreeText(raw.company(), 180);
        String location = sanitizer.sanitizeFreeText(raw.location(), 160);
        String seniority = sanitizer.sanitizeFreeText(raw.seniority(), 50);
        String description = sanitizer.sanitizeFreeText(raw.rawDescription(), 10000);
        String requirements = sanitizer.sanitizeFreeText(raw.requirements(), 10000);
        String sourceUrl = sanitizer.sanitizeFreeText(raw.sourceUrl(), 400);
        String sourceTenant = sanitizer.sanitizeFreeText(raw.sourceTenant(), 180);
        String remoteType = sanitizer.sanitizeFreeText(raw.remoteType(), 30);
        String employmentType = sanitizer.sanitizeFreeText(raw.employmentType(), 60);
        String externalJobId = sanitizer.sanitizeFreeText(raw.externalJobId(), 180);
        List<String> skills = (raw.requiredSkills() == null ? List.<String>of() : raw.requiredSkills()).stream()
                .map(value -> sanitizer.sanitizeFreeText(value, 40))
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeWhitespace)
                .limit(30)
                .toList();
        String canonicalTitle = canonicalizeTitle(title);
        String canonicalCompany = canonicalizeCompany(company);
        String canonicalLocation = canonicalizeLocation(location, raw.remote());
        String normalizedSeniority = normalizeSeniority(seniority);
        List<String> qualityFlags = computeQualityFlags(canonicalTitle, canonicalCompany, canonicalLocation, normalizedSeniority, skills, description, requirements, sourceUrl, raw.remote(), seniority);
        int qualityScore = computeQualityScore(canonicalTitle, canonicalCompany, canonicalLocation, normalizedSeniority, skills, description, requirements, sourceUrl);
        String dedupeKey = dedupeKey(canonicalTitle, canonicalCompany, canonicalLocation, raw.remote(), normalizedSeniority, skills);

        OffsetDateTime discoveredAt = OffsetDateTime.now();
        JsonNode normalizedPayload = buildNormalizedPayload(
                raw, externalJobId, title, canonicalTitle, company, canonicalCompany, location, canonicalLocation, seniority, normalizedSeniority,
                description, requirements, sourceUrl, sourceTenant, remoteType, employmentType, skills, qualityScore, qualityFlags, dedupeKey, discoveredAt
        );
        String checksum = checksum(raw.source(), sourceTenant, externalJobId, title, company, location, sourceUrl, description, requirements);
        if (sourceTenant == null || sourceTenant.isBlank()) {
            throw new IllegalArgumentException("sourceTenant obrigatorio para ingestao");
        }
        if (externalJobId == null || externalJobId.isBlank()) {
            throw new IllegalArgumentException("externalJobId obrigatorio para ingestao");
        }

        return new NormalizedVacancyRecord(
                platformId(raw.source()),
                raw.source(),
                sourceTenant,
                externalJobId,
                sourceUrl,
                checksum,
                title,
                canonicalTitle,
                company,
                canonicalCompany,
                location,
                canonicalLocation,
                remoteType,
                employmentType,
                raw.remote(),
                seniority,
                normalizedSeniority,
                skills,
                qualityScore,
                qualityFlags,
                dedupeKey,
                description,
                requirements,
                discoveredAt,
                raw.publishedAt(),
                normalizedPayload,
                raw.rawPayload()
        );
    }

    private JsonNode buildNormalizedPayload(ExternalVacancyRecord raw,
                                            String externalJobId,
                                            String title,
                                            String canonicalTitle,
                                            String company,
                                            String canonicalCompany,
                                            String location,
                                            String canonicalLocation,
                                            String seniority,
                                            String normalizedSeniority,
                                            String description,
                                            String requirements,
                                            String sourceUrl,
                                            String sourceTenant,
                                            String remoteType,
                                            String employmentType,
                                            List<String> skills,
                                            int qualityScore,
                                            List<String> qualityFlags,
                                            String dedupeKey,
                                            OffsetDateTime discoveredAt) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("source", raw.source().name());
        payload.put("sourceTenant", sourceTenant);
        payload.put("externalJobId", externalJobId);
        payload.put("sourceUrl", sourceUrl);
        payload.put("title", title);
        payload.put("canonicalTitle", canonicalTitle);
        payload.put("company", company);
        payload.put("canonicalCompany", canonicalCompany);
        payload.put("location", location);
        payload.put("canonicalLocation", canonicalLocation);
        payload.put("remoteType", remoteType);
        payload.put("employmentType", employmentType);
        payload.put("remote", raw.remote());
        payload.put("seniority", seniority);
        payload.put("normalizedSeniority", normalizedSeniority);
        payload.put("description", description);
        payload.put("requirements", requirements);
        payload.put("dedupeKey", dedupeKey);
        ObjectNode quality = payload.putObject("quality");
        quality.put("score", qualityScore);
        quality.set("flags", objectMapper.valueToTree(qualityFlags));
        payload.put("discoveredAt", discoveredAt.toString());
        if (raw.publishedAt() != null) {
            payload.put("publishedAt", raw.publishedAt().toString());
        }
        payload.set("requiredSkills", objectMapper.valueToTree(skills));
        payload.set("rawPayload", raw.rawPayload());
        return payload;
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return null;
        }
        return WHITESPACE.matcher(value.trim()).replaceAll(" ");
    }

    private String canonicalizeTitle(String value) {
        String normalized = normalizeWhitespace(stripMarkupTokens(value));
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String canonicalizeCompany(String value) {
        String normalized = normalizeWhitespace(stripMarkupTokens(value));
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        String noSuffix = COMPANY_SUFFIX.matcher(normalized).replaceAll("");
        String cleaned = normalizeWhitespace(noSuffix.replace(",", " "));
        return cleaned == null || cleaned.isBlank() ? normalized.toLowerCase(Locale.ROOT) : cleaned.toLowerCase(Locale.ROOT);
    }

    private String canonicalizeLocation(String location, boolean remote) {
        if (remote) {
            return "remote";
        }
        String normalized = normalizeWhitespace(stripMarkupTokens(location));
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeSeniority(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("junior") || lowered.contains("jr")) {
            return "junior";
        }
        if (lowered.contains("pleno") || lowered.contains("mid") || lowered.contains("middle")) {
            return "pleno";
        }
        if (lowered.contains("senior") || lowered.contains("sr")) {
            return "senior";
        }
        if (lowered.contains("staff") || lowered.contains("lead") || lowered.contains("principal") || lowered.contains("especialista")) {
            return "especialista";
        }
        return null;
    }

    private List<String> computeQualityFlags(String canonicalTitle,
                                             String canonicalCompany,
                                             String canonicalLocation,
                                             String normalizedSeniority,
                                             List<String> skills,
                                             String description,
                                             String requirements,
                                             String sourceUrl,
                                             boolean remote,
                                             String originalSeniority) {
        List<String> flags = new ArrayList<>();
        if (canonicalTitle == null) {
            flags.add("MISSING_TITLE");
        }
        if (canonicalCompany == null) {
            flags.add("MISSING_COMPANY");
        }
        if (!remote && canonicalLocation == null) {
            flags.add("MISSING_LOCATION");
        }
        if (normalizedSeniority == null) {
            flags.add("MISSING_OR_UNMAPPED_SENIORITY");
        } else if (originalSeniority != null && !originalSeniority.equalsIgnoreCase(normalizedSeniority)) {
            flags.add("SENIORITY_NORMALIZED");
        }
        if (skills == null || skills.isEmpty()) {
            flags.add("MISSING_SKILLS");
        }
        if (description == null || description.length() < 120) {
            flags.add("SHORT_DESCRIPTION");
        }
        if (requirements == null || requirements.length() < 80) {
            flags.add("SHORT_REQUIREMENTS");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            flags.add("MISSING_SOURCE_URL");
        }
        return List.copyOf(flags);
    }

    private int computeQualityScore(String canonicalTitle,
                                    String canonicalCompany,
                                    String canonicalLocation,
                                    String normalizedSeniority,
                                    List<String> skills,
                                    String description,
                                    String requirements,
                                    String sourceUrl) {
        int score = 0;
        if (canonicalTitle != null) {
            score += 20;
        }
        if (canonicalCompany != null) {
            score += 20;
        }
        if (canonicalLocation != null) {
            score += 15;
        }
        if (normalizedSeniority != null) {
            score += 15;
        }
        int skillCount = skills == null ? 0 : skills.size();
        if (skillCount >= 5) {
            score += 15;
        } else if (skillCount >= 2) {
            score += 10;
        } else if (skillCount == 1) {
            score += 5;
        }
        if (description != null && description.length() >= 120) {
            score += 10;
        }
        if (requirements != null && requirements.length() >= 80) {
            score += 5;
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            score += 5;
        }
        return Math.min(100, Math.max(0, score));
    }

    private String dedupeKey(String canonicalTitle,
                             String canonicalCompany,
                             String canonicalLocation,
                             boolean remote,
                             String normalizedSeniority,
                             List<String> skills) {
        String skillsKey = skills == null ? "" : skills.stream()
                .map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .limit(8)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        String key = "t:" + nullSafe(canonicalTitle)
                + "|c:" + nullSafe(canonicalCompany)
                + "|l:" + nullSafe(canonicalLocation)
                + "|r:" + remote
                + "|s:" + nullSafe(normalizedSeniority)
                + "|k:" + skillsKey;
        return key.length() > 512 ? key.substring(0, 512) : key;
    }

    private String stripMarkupTokens(String value) {
        if (value == null) {
            return null;
        }
        String unescapedTagsRemoved = HTML_ESCAPED_TAG.matcher(value).replaceAll(" ");
        return HTML_TAG.matcher(unescapedTagsRemoved).replaceAll(" ");
    }

    private String checksum(VacancyIngestionSource source,
                            String sourceTenant,
                            String externalJobId,
                            String title,
                            String company,
                            String location,
                            String sourceUrl,
                            String description,
                            String requirements) {
        String canonical = source.name() + "|"
                + nullSafe(sourceTenant) + "|"
                + nullSafe(externalJobId) + "|"
                + nullSafe(title) + "|"
                + nullSafe(company) + "|"
                + nullSafe(location) + "|"
                + nullSafe(sourceUrl) + "|"
                + nullSafe(description) + "|"
                + nullSafe(requirements);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao calcular checksum de vaga normalizada");
        }
    }

    private UUID platformId(VacancyIngestionSource source) {
        return switch (source) {
            case REMOTIVE -> REMOTIVE_PLATFORM_ID;
            case GREENHOUSE -> GREENHOUSE_PLATFORM_ID;
            case LEVER -> LEVER_PLATFORM_ID;
            case ADZUNA -> ADZUNA_PLATFORM_ID;
        };
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
