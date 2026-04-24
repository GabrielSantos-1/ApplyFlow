package com.applyflow.jobcopilot.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();
    private final Headers headers = new Headers();
    private final Actuator actuator = new Actuator();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Headers getHeaders() {
        return headers;
    }

    public Actuator getActuator() {
        return actuator;
    }

    public static class Jwt {
        private String issuer;
        private String audience;
        private String secretBase64;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getSecretBase64() {
            return secretBase64;
        }

        public void setSecretBase64(String secretBase64) {
            this.secretBase64 = secretBase64;
        }
    }

    public static class Cors {
        private String allowedOrigins;

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class RateLimit {
        private boolean redisEnabled = true;
        private boolean fallbackEnabled = true;
        private int windowSeconds = 60;
        private int loginLimit = 5;
        private int refreshLimit = 10;
        private int resumeUploadLimit = 6;
        private int resumeVariantLimit = 10;
        private int applicationDraftLimit = 12;
        private int vacancyReadLimit = 90;
        private int matchAnalysisLimit = 60;
        private int aiEnrichmentLimit = 20;
        private String redisKeyPrefix = "applyflow:ratelimit:";
        private boolean simulationEnabled = false;
        private String simulationMode = "none";
        private long simulatedLatencyMs = 0L;

        public boolean isRedisEnabled() {
            return redisEnabled;
        }

        public void setRedisEnabled(boolean redisEnabled) {
            this.redisEnabled = redisEnabled;
        }

        public boolean isFallbackEnabled() {
            return fallbackEnabled;
        }

        public void setFallbackEnabled(boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public int getLoginLimit() {
            return loginLimit;
        }

        public void setLoginLimit(int loginLimit) {
            this.loginLimit = loginLimit;
        }

        public int getRefreshLimit() {
            return refreshLimit;
        }

        public void setRefreshLimit(int refreshLimit) {
            this.refreshLimit = refreshLimit;
        }

        public int getResumeVariantLimit() {
            return resumeVariantLimit;
        }

        public int getResumeUploadLimit() {
            return resumeUploadLimit;
        }

        public void setResumeUploadLimit(int resumeUploadLimit) {
            this.resumeUploadLimit = resumeUploadLimit;
        }

        public void setResumeVariantLimit(int resumeVariantLimit) {
            this.resumeVariantLimit = resumeVariantLimit;
        }

        public int getApplicationDraftLimit() {
            return applicationDraftLimit;
        }

        public void setApplicationDraftLimit(int applicationDraftLimit) {
            this.applicationDraftLimit = applicationDraftLimit;
        }

        public int getVacancyReadLimit() {
            return vacancyReadLimit;
        }

        public void setVacancyReadLimit(int vacancyReadLimit) {
            this.vacancyReadLimit = vacancyReadLimit;
        }

        public int getMatchAnalysisLimit() {
            return matchAnalysisLimit;
        }

        public void setMatchAnalysisLimit(int matchAnalysisLimit) {
            this.matchAnalysisLimit = matchAnalysisLimit;
        }

        public int getAiEnrichmentLimit() {
            return aiEnrichmentLimit;
        }

        public void setAiEnrichmentLimit(int aiEnrichmentLimit) {
            this.aiEnrichmentLimit = aiEnrichmentLimit;
        }

        public String getRedisKeyPrefix() {
            return redisKeyPrefix;
        }

        public void setRedisKeyPrefix(String redisKeyPrefix) {
            this.redisKeyPrefix = redisKeyPrefix;
        }

        public boolean isSimulationEnabled() {
            return simulationEnabled;
        }

        public void setSimulationEnabled(boolean simulationEnabled) {
            this.simulationEnabled = simulationEnabled;
        }

        public String getSimulationMode() {
            return simulationMode;
        }

        public void setSimulationMode(String simulationMode) {
            this.simulationMode = simulationMode;
        }

        public long getSimulatedLatencyMs() {
            return simulatedLatencyMs;
        }

        public void setSimulatedLatencyMs(long simulatedLatencyMs) {
            this.simulatedLatencyMs = simulatedLatencyMs;
        }
    }

    public static class Headers {
        private String mode = "dev";
        private boolean hstsEnabled;
        private final Csp csp = new Csp();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public Csp getCsp() {
            return csp;
        }

        public String resolveCspPolicy() {
            if ("prod".equalsIgnoreCase(mode)) {
                return csp.getProd();
            }
            return csp.getDev();
        }
    }

    public static class Csp {
        private String dev;
        private String prod;

        public String getDev() {
            return dev;
        }

        public void setDev(String dev) {
            this.dev = dev;
        }

        public String getProd() {
            return prod;
        }

        public void setProd(String prod) {
            this.prod = prod;
        }
    }

    public static class Actuator {
        private String metricsToken;

        public String getMetricsToken() {
            return metricsToken;
        }

        public void setMetricsToken(String metricsToken) {
            this.metricsToken = metricsToken;
        }
    }
}
