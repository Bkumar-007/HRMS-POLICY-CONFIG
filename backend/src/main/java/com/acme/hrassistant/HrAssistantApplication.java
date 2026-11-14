package com.acme.hrassistant;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Phase 1: chat + JWT only. PgVector auto-config is excluded until RAG (Phase 3)
 * when an EmbeddingModel bean is wired.
 */
@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class HrAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrAssistantApplication.class, args);
    }
}