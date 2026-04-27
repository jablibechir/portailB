package com.soprarh.portail.scoring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration du WebClient pour appeler le microservice Python (scoring IA).
 */
@Configuration
public class AiServiceConfig {

    @Value("${python.service.base-url}")
    private String pythonServiceBaseUrl;

    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl(pythonServiceBaseUrl)
                .build();
    }
}

