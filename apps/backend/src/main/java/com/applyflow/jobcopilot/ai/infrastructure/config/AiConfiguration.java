package com.applyflow.jobcopilot.ai.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    RestClient aiRestClient(AiProperties aiProperties) {
        AiProperties.Provider cfg = aiProperties.getProvider();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(cfg.getReadTimeoutMs()));
        return RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}

