package com.acme.hrassistant.config;

import com.acme.hrassistant.agent.HrTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are Acme Corp's HR Policy Assistant (demo portfolio app).
            Be concise and helpful. Address the employee by display name when greeting.

            You have tools:
            - getLeaveBalance — remaining casual / sick / earned leave for the logged-in employee
            - getHolidayList — company holidays by year (and optional month)
            - searchHrPolicies — retrieve passages from the uploadable HR policy library (any topic)

            Rules:
            - For leave balance or holiday questions, ALWAYS call the appropriate tool. Never invent numbers or dates.
            - For policy / handbook / eligibility / "can I..." questions, ALWAYS call searchHrPolicies first.
              Answer ONLY from that context. If context is missing, say you could not find it in indexed policies.
            - The employee identity is already known server-side. Never ask for or accept another employee's id.
            - Leave application (with confirmation) arrives in Phase 4; if asked to apply leave, explain that
              and still help with balance / policy Q&A.
            - If a tool returns no data, say so clearly.
            """;

    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, HrTools hrTools) {
        return chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(hrTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
