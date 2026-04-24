package com.applyflow.jobcopilot.ai.domain;

public enum AiFlow {
    MATCH_ENRICHMENT("match-enrichment"),
    CV_IMPROVEMENT("cv-improvement"),
    APPLICATION_DRAFT("application-draft");

    private final String metricTag;

    AiFlow(String metricTag) {
        this.metricTag = metricTag;
    }

    public String metricTag() {
        return metricTag;
    }
}

