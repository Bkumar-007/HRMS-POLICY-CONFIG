package com.acme.hrassistant.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.hrassistant.eval.AnswerQualityEval.Item;
import com.acme.hrassistant.eval.AnswerQualityEval.Outcome;
import com.acme.hrassistant.eval.AnswerQualityEval.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnswerQualityEvalTest {

    @Test
    void datasetCoversAnswerableAndRefusalCases() throws Exception {
        List<Item> items = AnswerQualityEval.load(new ObjectMapper());
        assertTrue(items.size() >= 12, "Expected ~15 items, found " + items.size());

        Set<String> ids = new HashSet<>();
        for (Item item : items) {
            assertTrue(ids.add(item.id()), "Duplicate id " + item.id());
            assertFalse(item.question().isBlank(), "Empty question " + item.id());
            if (item.type() == Type.ANSWERABLE) {
                assertFalse(item.expectedCitations().isEmpty(), "Answerable item needs citations: " + item.id());
                assertFalse(item.requiredFacts().isEmpty(), "Answerable item needs facts: " + item.id());
            } else {
                assertTrue(item.expectedCitations().isEmpty(), "Refusal item must not expect citations: " + item.id());
            }
        }
        assertTrue(items.stream().anyMatch(i -> i.type() == Type.NOT_IN_POLICY));
        assertTrue(items.stream().anyMatch(i -> i.type() == Type.ANSWERABLE));
    }

    @Test
    void answerableItemPassesWithExpectedCitationAndFacts() {
        Item item = new Item("T1", Type.ANSWERABLE, "priya.employee", "q",
                List.of("Leave Policy#Casual Leave"), List.of("12"), List.of());
        Outcome ok = AnswerQualityEval.score(
                item, "You get 12 casual leave days.", List.of("Leave Policy#Casual Leave"), false);
        assertTrue(ok.passed());

        Outcome wrongCitation = AnswerQualityEval.score(
                item, "You get 12 casual leave days.", List.of("WFH Policy 2026#Core Hours"), false);
        assertFalse(wrongCitation.passed());
        assertEquals(Boolean.FALSE, wrongCitation.citationsOk());
    }

    @Test
    void forbiddenClaimFailsEvenWhenCitationMatches() {
        Item item = new Item("T2", Type.ANSWERABLE, "priya.employee", "q",
                List.of("Travel Policy#Travel Advance"), List.of("15,000"), List.of("25,000"));
        Outcome outcome = AnswerQualityEval.score(
                item, "Limit is INR 15,000 (G4 gets 25,000).", List.of("Travel Policy#Travel Advance"), false);
        assertFalse(outcome.passed());
    }

    @Test
    void refusalPassesOnDeclineOrHelpdeskEscalationButNotOnInventedAnswer() {
        Item item = new Item("T3", Type.NOT_IN_POLICY, "priya.employee", "q", List.of(), List.of(), List.of());

        assertTrue(AnswerQualityEval.score(item, "I could not find that in the indexed policies.", List.of(), false).passed());
        assertTrue(AnswerQualityEval.score(item, "Please confirm the ticket.", List.of(), true).passed());
        assertFalse(AnswerQualityEval.score(item, "The gift limit is INR 5,000.", List.of(), false).passed());
    }
}
