package com.acme.hrassistant.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loader for {@code eval/retrieval-eval.json}. Relevant items are section labels of the form
 * {@code <sourceFile>#<heading>} because chunk UUIDs change on every ingest.
 */
public final class RetrievalEvalDataset {

    public static final String FILE_NAME = "retrieval-eval.json";

    private RetrievalEvalDataset() {
    }

    public record Question(
            String id,
            String question,
            Set<String> relevantDocSections,
            String employeeGrade,
            String employeeLocation) {
    }

    public static Path path() {
        return EvalFiles.resolve(FILE_NAME);
    }

    public static List<Question> load(ObjectMapper objectMapper) throws IOException {
        return load(objectMapper, path());
    }

    public static List<Question> load(ObjectMapper objectMapper, Path file) throws IOException {
        JsonNode root = objectMapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
        String defaultGrade = root.path("defaultEmployeeGrade").asText("");
        String defaultLocation = root.path("defaultEmployeeLocation").asText("");
        JsonNode questions = root.get("questions");
        if (questions == null || !questions.isArray()) {
            throw new IllegalStateException("'questions' array missing in " + file);
        }
        List<Question> out = new ArrayList<>();
        for (JsonNode node : questions) {
            Set<String> relevant = new LinkedHashSet<>();
            JsonNode sections = node.get("relevantDocSections");
            if (sections != null && sections.isArray()) {
                sections.forEach(s -> relevant.add(s.asText()));
            }
            out.add(new Question(
                    node.path("id").asText(),
                    node.path("question").asText(),
                    relevant,
                    node.path("employeeGrade").asText(defaultGrade),
                    node.path("employeeLocation").asText(defaultLocation)));
        }
        return out;
    }
}
