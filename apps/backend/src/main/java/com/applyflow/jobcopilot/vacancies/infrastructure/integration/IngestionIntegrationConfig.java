package com.applyflow.jobcopilot.vacancies.infrastructure.integration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IngestionProperties.class)
public class IngestionIntegrationConfig {

    @Bean
    RestClient remotiveRestClient(IngestionProperties properties) {
        IngestionProperties.Remotive cfg = properties.getSources().getRemotive();
        return buildClient(cfg.getBaseUrl(), cfg.getConnectTimeoutMs(), cfg.getReadTimeoutMs());
    }

    @Bean
    RestClient greenhouseRestClient(IngestionProperties properties) {
        IngestionProperties.Greenhouse cfg = properties.getSources().getGreenhouse();
        return buildClient(cfg.getBaseUrl(), cfg.getConnectTimeoutMs(), cfg.getReadTimeoutMs());
    }

    @Bean
    RestClient leverRestClient(IngestionProperties properties) {
        IngestionProperties.Lever cfg = properties.getSources().getLever();
        return buildClient(cfg.getBaseUrl(), cfg.getConnectTimeoutMs(), cfg.getReadTimeoutMs());
    }

    @Bean
    RestClient adzunaRestClient(IngestionProperties properties) {
        IngestionProperties.Adzuna cfg = properties.getSources().getAdzuna();
        return buildClient(cfg.getBaseUrl(), cfg.getConnectTimeoutMs(), cfg.getReadTimeoutMs());
    }

    private RestClient buildClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
