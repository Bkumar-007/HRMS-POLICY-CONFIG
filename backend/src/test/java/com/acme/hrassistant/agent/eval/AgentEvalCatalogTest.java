package com.acme.hrassistant.agent.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.hrassistant.agent.eval.AgentEvalCatalog.Scenario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Structural validation of {@code eval/agent-eval.json}. This test deliberately does NOT score
 * the agent: tool-selection quality is measured live via {@link AgentEvalRunner} (profile
 * {@code eval-live}) through ChatService and the tool-call audit log.
 */
class AgentEvalCatalogTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void catalogHasEnoughScenariosWithUniqueIdsAndNonEmptyPrompts() throws Exception {
        List<Scenario> scenarios = AgentEvalCatalog.load(MAPPER);
        assertTrue(scenarios.size() >= 35, "Need 35+ agent eval scenarios, found " + scenarios.size());

        Set<String> ids = new HashSet<>();
        for (Scenario scenario : scenarios) {
            assertFalse(scenario.id().isBlank(), "Scenario with blank id");
            assertTrue(ids.add(scenario.id()), "Duplicate scenario id " + scenario.id());
            assertFalse(scenario.prompt().isBlank(), "Empty prompt in " + scenario.id());
        }
    }

    @Test
    void everyScenarioDeclaresExpectedAndForbiddenToolsFromKnownSet() throws Exception {
        JsonNode root = MAPPER.readTree(Files.readString(AgentEvalCatalog.path(), StandardCharsets.UTF_8));
        for (JsonNode scenario : root.get("scenarios")) {
            String id = scenario.path("id").asText();
            assertTrue(scenario.path("expectedTools").isArray(), "expectedTools array missing in " + id);
            assertTrue(scenario.path("forbiddenTools").isArray(), "forbiddenTools array missing in " + id);
        }

        for (Scenario scenario : AgentEvalCatalog.load(MAPPER)) {
            for (String tool : scenario.expectedTools()) {
                assertTrue(AgentEvalCatalog.KNOWN_TOOLS.contains(tool),
                        "Unknown expected tool in " + scenario.id() + ": " + tool);
            }
            for (String tool : scenario.forbiddenTools()) {
                assertTrue(AgentEvalCatalog.KNOWN_TOOLS.contains(tool),
                        "Unknown forbidden tool in " + scenario.id() + ": " + tool);
            }
            assertFalse(
                    scenario.expectedTools().stream().anyMatch(scenario.forbiddenTools()::contains),
                    "Tool is both expected and forbidden in " + scenario.id());
        }
    }

    @Test
    void catalogCoversEveryAgentTool() throws Exception {
        Set<String> covered = new HashSet<>();
        for (Scenario scenario : AgentEvalCatalog.load(MAPPER)) {
            covered.addAll(scenario.expectedTools());
        }
        assertTrue(covered.containsAll(AgentEvalCatalog.KNOWN_TOOLS),
                "Catalog never expects: " + difference(AgentEvalCatalog.KNOWN_TOOLS, covered));
    }

    @Test
    void keywordHeuristicScoringWasRemoved() {
        // The old predictTools() keyword matcher was scored against the same prompts it was written
        // from, so its hit-rate said nothing about the agent. Guard against reintroducing it.
        for (Class<?> type : List.of(AgentEvalRunner.class, AgentEvalCatalogTest.class, AgentEvalScorer.class)) {
            boolean hasPredictTools = Arrays.stream(type.getDeclaredMethods())
                    .map(Method::getName)
                    .anyMatch("predictTools"::equals);
            assertFalse(hasPredictTools, type.getSimpleName() + " must not define predictTools");
        }
        assertThrows(NoSuchMethodException.class, () -> AgentEvalRunner.class.getDeclaredMethod("predictTools", String.class));
    }

    private static Set<String> difference(Set<String> all, Set<String> covered) {
        Set<String> missing = new HashSet<>(all);
        missing.removeAll(covered);
        return missing;
    }
}
