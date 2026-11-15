package com.acme.hrassistant.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit Gemini HTTP timeout so chat fails fast instead of hanging for minutes.
 */
@Configuration
@org.springframework.context.annotation.Profile("!test")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai", matchIfMissing = true)
public class LlmClientConfig {

    @Bean
    @ConditionalOnMissingBean(Client.class)
    Client googleGenAiClient(GoogleGenAiConnectionProperties connectionProperties, AppProperties appProperties) {
        var builder = Client.builder();
        if (connectionProperties.getApiKey() != null && !connectionProperties.getApiKey().isBlank()) {
            builder.apiKey(connectionProperties.getApiKey());
        }
        if (connectionProperties.isVertexAi()) {
            builder.project(connectionProperties.getProjectId())
                    .location(connectionProperties.getLocation())
                    .vertexAI(true);
        }
        builder.httpOptions(HttpOptions.builder()
                .timeout(appProperties.getLlm().getReadTimeoutMs())
                .build());
        return builder.build();
    }
}
