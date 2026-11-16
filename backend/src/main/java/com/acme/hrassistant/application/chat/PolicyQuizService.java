package com.acme.hrassistant.application.chat;

import com.acme.hrassistant.rag.PolicyMetadataKeys;
import com.acme.hrassistant.rag.PolicySearchService;
import com.acme.hrassistant.web.dto.ChatResponse.QuizQuestionDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/**
 * Builds short training quizzes from retrieved policy passages (no extra LLM call).
 */
@Service
public class PolicyQuizService {

    private final PolicySearchService policySearchService;

    public PolicyQuizService(PolicySearchService policySearchService) {
        this.policySearchService = policySearchService;
    }

    public List<QuizQuestionDto> generate(String topic, String grade, String location) {
        PolicySearchService.SearchResult result = policySearchService.search(topic, grade, location);
        List<Document> docs = result.documents();
        if (docs.isEmpty()) {
            return List.of();
        }

        List<QuizQuestionDto> questions = new ArrayList<>();
        int limit = Math.min(3, docs.size());
        for (int i = 0; i < limit; i++) {
            Document doc = docs.get(i);
            String policyName = meta(doc, PolicyMetadataKeys.POLICY_NAME, "HR Policy");
            String section = meta(doc, PolicyMetadataKeys.SECTION, "General");
            String sentence = firstSentence(doc.getText());
            if (sentence.isBlank()) {
                continue;
            }

            String distractorA = "This rule applies only to contractors, never employees.";
            String distractorB = "Employees may ignore this section without manager approval.";
            List<String> options = new ArrayList<>();
            options.add(sentence);
            options.add(distractorA);
            options.add(distractorB);

            questions.add(new QuizQuestionDto(
                    UUID.randomUUID().toString(),
                    "According to %s (%s), which statement is correct?".formatted(policyName, section),
                    List.copyOf(options),
                    0,
                    "Taken from indexed policy text in section \"%s\".".formatted(section),
                    policyName,
                    section));
        }
        return questions;
    }

    private static String meta(Document doc, String key, String fallback) {
        Object raw = doc.getMetadata().get(key);
        if (raw == null) {
            return fallback;
        }
        String value = String.valueOf(raw).trim();
        return value.isBlank() ? fallback : value;
    }

    private static String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        int end = -1;
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                end = i;
                break;
            }
        }
        String sentence = end > 20 ? compact.substring(0, end + 1) : compact;
        if (sentence.length() > 220) {
            sentence = sentence.substring(0, 217) + "...";
        }
        // Prefer a statement that looks like policy prose, not a heading
        if (sentence.toLowerCase(Locale.ROOT).startsWith("policy:")) {
            return compact.length() <= 220 ? compact : compact.substring(0, 217) + "...";
        }
        return sentence;
    }
}
