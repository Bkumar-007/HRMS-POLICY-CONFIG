package com.acme.hrassistant.agent.eval;

import com.acme.hrassistant.eval.EvalFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Loader for {@code eval/agent-eval.json}: the labeled prompt → expected/forbidden tool catalog.
 * The catalog is only data; agent quality is measured by running the prompts through ChatService.
 */
public final class AgentEvalCatalog {

    public static final String FILE_NAME = "agent-eval.json";

    public static final Set<String> KNOWN_TOOLS = Set.of(
            "getLeaveBalance",
            "getHolidayList",
            "searchHrPolicies",
            "startPolicyQuiz",
            "applyLeave",
            "raiseHelpdeskTicket");

    private AgentEvalCatalog() {
    }

    public record Scenario(
            String id,
            String prompt,
            List<String> expectedTools,
            List<String> forbiddenTools,
            String notes) {
    }

    public static Path path() {
        return EvalFiles.resolve(FILE_NAME);
    }

    public static List<Scenario> load(ObjectMapper objectMapper) throws IOException {
        return load(objectMapper, path());
    }

    public static List<Scenario> load(ObjectMapper objectMapper, Path catalog) throws IOException {
        JsonNode root = objectMapper.readTree(Files.readString(catalog, StandardCharsets.UTF_8));
        JsonNode scenarios = root.get("scenarios");
        if (scenarios == null || !scenarios.isArray()) {
            throw new IllegalStateException("'scenarios' array missing in " + catalog);
        }
        List<Scenario> out = new ArrayList<>();
        for (JsonNode node : scenarios) {
            out.add(new Scenario(
                    node.path("id").asText(),
                    node.path("prompt").asText(),
                    toList(node.get("expectedTools")),
                    toList(node.get("forbiddenTools")),
                    node.path("notes").asText("")));
        }
        return out;
    }

    private static List<String> toList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
