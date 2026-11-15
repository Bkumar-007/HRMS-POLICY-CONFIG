package com.acme.hrassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HR Policy Assistant — chat, tools, and RAG over an uploadable policy library (pgvector).
 */
@SpringBootApplication
public class HrAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrAssistantApplication.class, args);
    }
}
