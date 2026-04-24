package com.applyflow.jobcopilot.vacancies.infrastructure.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {
    private boolean enabled = true;
    private final Scheduler scheduler = new Scheduler();
    private final Bootstrap bootstrap = new Bootstrap();
    private final Sources sources = new Sources();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public Sources getSources() {
        return sources;
    }

    public static class Bootstrap {
        private boolean enabled = false;
        private String source = "REMOTIVE";
        private int maxJobsPerRun = 120;
        private boolean onlyWhenVacanciesEmpty = true;
        private boolean requireNoRuns = true;
        private List<String> allowedProfiles = new ArrayList<>(List.of("staging", "dev"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public int getMaxJobsPerRun() {
            return maxJobsPerRun;
        }

        public void setMaxJobsPerRun(int maxJobsPerRun) {
            this.maxJobsPerRun = maxJobsPerRun;
        }

        public boolean isOnlyWhenVacanciesEmpty() {
            return onlyWhenVacanciesEmpty;
        }

        public void setOnlyWhenVacanciesEmpty(boolean onlyWhenVacanciesEmpty) {
            this.onlyWhenVacanciesEmpty = onlyWhenVacanciesEmpty;
        }

        public boolean isRequireNoRuns() {
            return requireNoRuns;
        }

        public void setRequireNoRuns(boolean requireNoRuns) {
            this.requireNoRuns = requireNoRuns;
        }

        public List<String> getAllowedProfiles() {
            return allowedProfiles;
        }

        public void setAllowedProfiles(List<String> allowedProfiles) {
            this.allowedProfiles = allowedProfiles == null ? List.of() : allowedProfiles;
        }

        public String normalizedSource() {
            return source == null ? "REMOTIVE" : source.trim().toUpperCase(Locale.ROOT);
        }
    }

    public static class Scheduler {
        private boolean enabled = false;
        private long remotiveFixedDelayMs = 1800000L;
        private long greenhouseFixedDelayMs = 3600000L;
        private long leverFixedDelayMs = 3600000L;
        private long adzunaFixedDelayMs = 3600000L;
        private long jobSearchPreferencesFixedDelayMs = 3600000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getRemotiveFixedDelayMs() {
            return remotiveFixedDelayMs;
        }

        public void setRemotiveFixedDelayMs(long remotiveFixedDelayMs) {
            this.remotiveFixedDelayMs = remotiveFixedDelayMs;
        }

        public long getGreenhouseFixedDelayMs() {
            return greenhouseFixedDelayMs;
        }

        public void setGreenhouseFixedDelayMs(long greenhouseFixedDelayMs) {
            this.greenhouseFixedDelayMs = greenhouseFixedDelayMs;
        }

        public long getLeverFixedDelayMs() {
            return leverFixedDelayMs;
        }

        public void setLeverFixedDelayMs(long leverFixedDelayMs) {
            this.leverFixedDelayMs = leverFixedDelayMs;
        }

        public long getAdzunaFixedDelayMs() {
            return adzunaFixedDelayMs;
        }

        public void setAdzunaFixedDelayMs(long adzunaFixedDelayMs) {
            this.adzunaFixedDelayMs = adzunaFixedDelayMs;
        }

        public long getJobSearchPreferencesFixedDelayMs() {
            return jobSearchPreferencesFixedDelayMs;
        }

        public void setJobSearchPreferencesFixedDelayMs(long jobSearchPreferencesFixedDelayMs) {
            this.jobSearchPreferencesFixedDelayMs = jobSearchPreferencesFixedDelayMs;
        }
    }

    public static class Sources {
        private final Remotive remotive = new Remotive();
        private final Greenhouse greenhouse = new Greenhouse();
        private final Lever lever = new Lever();
        private final Adzuna adzuna = new Adzuna();

        public Remotive getRemotive() {
            return remotive;
        }

        public Greenhouse getGreenhouse() {
            return greenhouse;
        }

        public Lever getLever() {
            return lever;
        }

        public Adzuna getAdzuna() {
            return adzuna;
        }
    }

    public static class Remotive {
        private boolean enabled = true;
        private String baseUrl = "https://remotive.com";
        private String jobsPath = "/api/remote-jobs";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
        private int maxPayloadBytes = 1_500_000;
        private int maxJobsPerRun = 300;
        private List<String> allowedHosts = new ArrayList<>(List.of("remotive.com"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getJobsPath() {
            return jobsPath;
        }

        public void setJobsPath(String jobsPath) {
            this.jobsPath = jobsPath;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public int getMaxPayloadBytes() {
            return maxPayloadBytes;
        }

        public void setMaxPayloadBytes(int maxPayloadBytes) {
            this.maxPayloadBytes = maxPayloadBytes;
        }

        public int getMaxJobsPerRun() {
            return maxJobsPerRun;
        }

        public void setMaxJobsPerRun(int maxJobsPerRun) {
            this.maxJobsPerRun = maxJobsPerRun;
        }

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
        }
    }

    public static class Greenhouse {
        private boolean enabled = false;
        private String baseUrl = "https://boards-api.greenhouse.io";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
        private int maxPayloadBytes = 1_500_000;
        private int maxJobsPerRun = 200;
        private List<String> allowedHosts = new ArrayList<>(List.of("boards-api.greenhouse.io"));

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public int getMaxPayloadBytes() { return maxPayloadBytes; }
        public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = maxPayloadBytes; }
        public int getMaxJobsPerRun() { return maxJobsPerRun; }
        public void setMaxJobsPerRun(int maxJobsPerRun) { this.maxJobsPerRun = maxJobsPerRun; }
        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
    }

    public static class Lever {
        private boolean enabled = false;
        private String baseUrl = "https://api.lever.co";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
        private int maxPayloadBytes = 1_500_000;
        private int maxJobsPerRun = 200;
        private List<String> allowedHosts = new ArrayList<>(List.of("api.lever.co"));

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public int getMaxPayloadBytes() { return maxPayloadBytes; }
        public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = maxPayloadBytes; }
        public int getMaxJobsPerRun() { return maxJobsPerRun; }
        public void setMaxJobsPerRun(int maxJobsPerRun) { this.maxJobsPerRun = maxJobsPerRun; }
        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
    }

    public static class Adzuna {
        private boolean enabled = false;
        private String baseUrl = "https://api.adzuna.com";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
        private int maxPayloadBytes = 1_500_000;
        private int maxJobsPerRun = 200;
        private List<String> allowedHosts = new ArrayList<>(List.of("api.adzuna.com"));
        private String appId = "";
        private String appKey = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public int getMaxPayloadBytes() { return maxPayloadBytes; }
        public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = maxPayloadBytes; }
        public int getMaxJobsPerRun() { return maxJobsPerRun; }
        public void setMaxJobsPerRun(int maxJobsPerRun) { this.maxJobsPerRun = maxJobsPerRun; }
        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
    }
}
