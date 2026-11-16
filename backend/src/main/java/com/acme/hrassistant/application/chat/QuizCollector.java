package com.acme.hrassistant.application.chat;

import com.acme.hrassistant.web.dto.ChatResponse.QuizQuestionDto;
import java.util.ArrayList;
import java.util.List;

/** Request-scoped collector filled by {@code startPolicyQuiz}; read by ChatService. */
public class QuizCollector {

    private final List<QuizQuestionDto> questions = new ArrayList<>();

    public void addAll(List<QuizQuestionDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        questions.addAll(items);
    }

    public List<QuizQuestionDto> getQuestions() {
        return List.copyOf(questions);
    }
}
