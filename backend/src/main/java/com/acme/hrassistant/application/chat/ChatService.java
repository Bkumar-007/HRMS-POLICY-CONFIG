package com.acme.hrassistant.application.chat;

import com.acme.hrassistant.agent.HrToolContextKeys;
import com.acme.hrassistant.application.actions.PendingActionCollector;
import com.acme.hrassistant.application.actions.ToolIterationGuard;
import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.rag.CitationCollector;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.ChatRequest;
import com.acme.hrassistant.web.dto.ChatResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Gemini chat with HR tools (leave, holidays, policy RAG, quiz, gated writes),
 * JDBC memory, citations, multilingual replies.
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatConversationService chatConversationService;
    private final AppProperties appProperties;
    private final MeterRegistry meterRegistry;

    public ChatService(
            ChatClient chatClient,
            ChatConversationService chatConversationService,
            AppProperties appProperties,
            MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.chatConversationService = chatConversationService;
        this.appProperties = appProperties;
        this.meterRegistry = meterRegistry;
    }

    public ChatResponse chat(EmployeePrincipal user, ChatRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String conversationId = chatConversationService.resolveConversationId(
                    user.getEmployeeId(), request.conversationId());

            CitationCollector citationCollector = new CitationCollector();
            PendingActionCollector pendingActionCollector = new PendingActionCollector();
            QuizCollector quizCollector = new QuizCollector();
            ToolIterationGuard iterationGuard =
                    new ToolIterationGuard(appProperties.getAgent().getMaxToolIterations());

            Map<String, Object> toolContext = new HashMap<>();
            toolContext.put(HrToolContextKeys.EMPLOYEE_ID, user.getEmployeeId());
            toolContext.put(HrToolContextKeys.CONVERSATION_ID, conversationId);
            toolContext.put(HrToolContextKeys.EMPLOYEE_LOCATION, user.getLocation());
            toolContext.put(HrToolContextKeys.EMPLOYEE_GRADE, user.getGrade());
            toolContext.put(HrToolContextKeys.CITATION_COLLECTOR, citationCollector);
            toolContext.put(HrToolContextKeys.PENDING_ACTION_COLLECTOR, pendingActionCollector);
            toolContext.put(HrToolContextKeys.QUIZ_COLLECTOR, quizCollector);
            toolContext.put(HrToolContextKeys.TOOL_ITERATION_GUARD, iterationGuard);

            String language = normalizeLanguage(request.preferredLanguage(), request.message());

            String reply = chatClient.prompt()
                    .user(u -> u.text("""
                            Employee: %s (grade=%s, location=%s, role=%s)
                            Preferred reply language: %s
                            Message: %s
                            """.formatted(
                            user.getDisplayName(),
                            user.getGrade(),
                            user.getLocation(),
                            user.getRole(),
                            language,
                            request.message())))
                    .toolContext(toolContext)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();

            return new ChatResponse(
                    conversationId,
                    reply == null ? "" : reply,
                    citationCollector.getCitations(),
                    pendingActionCollector.getActions(),
                    quizCollector.getQuestions());
        } finally {
            sample.stop(meterRegistry.timer("hr.chat.latency"));
        }
    }

    static String normalizeLanguage(String preferred, String message) {
        String raw = preferred == null ? "AUTO" : preferred.trim().toUpperCase(Locale.ROOT);
        if ("EN".equals(raw) || "ENGLISH".equals(raw)) {
            return "EN";
        }
        if ("HI".equals(raw) || "HINDI".equals(raw)) {
            return "HI";
        }
        if (message != null && message.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.DEVANAGARI)) {
            return "HI";
        }
        return "AUTO";
    }
}
