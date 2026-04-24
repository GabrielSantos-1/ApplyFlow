package com.applyflow.jobcopilot.resumes.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resumes.storage")
public class ResumeStorageProperties {
    private String baseDir = "./data/private/resumes";
    private long maxPdfBytes = 5_242_880L;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public long getMaxPdfBytes() {
        return maxPdfBytes;
    }

    public void setMaxPdfBytes(long maxPdfBytes) {
        this.maxPdfBytes = maxPdfBytes;
    }
}
