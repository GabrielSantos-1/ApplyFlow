package com.applyflow.jobcopilot.matching.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_results", uniqueConstraints = {
        @UniqueConstraint(name = "uq_match_results_user_vacancy", columnNames = {"user_id", "vacancy_id"})
})
public class MatchResultJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "vacancy_id")
    private UUID vacancyId;
    @Column(name = "resume_variant_id")
    private UUID resumeVariantId;
    @Column(name = "resume_id")
    private UUID resumeId;
    private Short score;
    @Column(name = "recommendation")
    private String recommendation;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown")
    private JsonNode scoreBreakdown;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths_json")
    private JsonNode strengthsJson;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gaps_json")
    private JsonNode gapsJson;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords_to_add_json")
    private JsonNode keywordsToAddJson;
    @Column(name = "algorithm_version")
    private String algorithmVersion;
    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getVacancyId() { return vacancyId; }
    public void setVacancyId(UUID vacancyId) { this.vacancyId = vacancyId; }
    public UUID getResumeVariantId() { return resumeVariantId; }
    public void setResumeVariantId(UUID resumeVariantId) { this.resumeVariantId = resumeVariantId; }
    public UUID getResumeId() { return resumeId; }
    public void setResumeId(UUID resumeId) { this.resumeId = resumeId; }
    public Short getScore() { return score; }
    public void setScore(Short score) { this.score = score; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public JsonNode getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(JsonNode scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }
    public JsonNode getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(JsonNode strengthsJson) { this.strengthsJson = strengthsJson; }
    public JsonNode getGapsJson() { return gapsJson; }
    public void setGapsJson(JsonNode gapsJson) { this.gapsJson = gapsJson; }
    public JsonNode getKeywordsToAddJson() { return keywordsToAddJson; }
    public void setKeywordsToAddJson(JsonNode keywordsToAddJson) { this.keywordsToAddJson = keywordsToAddJson; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
