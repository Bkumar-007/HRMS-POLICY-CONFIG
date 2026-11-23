package com.acme.hrassistant.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.hrassistant.eval.RetrievalEvalDataset.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Keeps eval/retrieval-eval.json honest: every label must point at a real seed-policy heading. */
class RetrievalEvalDatasetTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void datasetHasThirtyToFortyQuestionsWithUniqueIdsAndLabels() throws Exception {
        List<Question> questions = RetrievalEvalDataset.load(MAPPER);
        assertTrue(questions.size() >= 30 && questions.size() <= 40,
                "Expected 30-40 questions, found " + questions.size());

        Set<String> ids = new HashSet<>();
        for (Question q : questions) {
            assertTrue(ids.add(q.id()), "Duplicate id " + q.id());
            assertFalse(q.question().isBlank(), "Empty question " + q.id());
            assertFalse(q.relevantDocSections().isEmpty(), "No relevant sections for " + q.id());
        }
    }

    @Test
    void everyRelevantSectionExistsInSeedPolicies() throws Exception {
        for (Question q : RetrievalEvalDataset.load(MAPPER)) {
            for (String label : q.relevantDocSections()) {
                int hash = label.indexOf('#');
                assertTrue(hash > 0 && hash < label.length() - 1, "Bad label '" + label + "' in " + q.id());
                String file = label.substring(0, hash);
                String heading = label.substring(hash + 1);

                String content = readSeedPolicy(file);
                assertTrue(
                        content.lines().anyMatch(line -> line.trim().equals("## " + heading)),
                        q.id() + ": no heading '## " + heading + "' in seed-policies/" + file);
            }
        }
    }

    private static String readSeedPolicy(String file) throws Exception {
        try (InputStream in = RetrievalEvalDatasetTest.class.getResourceAsStream("/seed-policies/" + file)) {
            assertTrue(in != null, "Missing seed policy " + file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
