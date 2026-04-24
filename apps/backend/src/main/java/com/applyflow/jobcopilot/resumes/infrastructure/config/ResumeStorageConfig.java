package com.applyflow.jobcopilot.resumes.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResumeStorageProperties.class)
public class ResumeStorageConfig {
}
