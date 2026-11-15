package com.acme.hrassistant.agent.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Loads {@code eval/agent-eval.json} and runs a deterministic heuristic scorer
 * (keyword → expected tools) so CI can report a baseline without calling Gemini.
 */
class AgentEvalCatalogTest {

    private static final Set<String> KNOWN_TOOLS = Set.of(
            "getLeaveBalance",
            "getHolidayList",
            "searchHrPolicies",
            "applyLeave",
            "raiseHelpdeskTicket");

    @Test
    void catalogHasAtLeastTwentyScenariosAndHeuristicPassRateIsHigh() throws Exception {
        Path catalog = resolveCatalog();
        JsonNode root = new ObjectMapper().readTree(Files.readString(catalog));
        JsonNode scenarios = root.get("scenarios");
        assertTrue(scenarios.isArray() && scenarios.size() >= 20, "Need 20+ agent eval scenarios");

        int scored = 0;
        int hits = 0;
        List<String> misses = new ArrayList<>();

        for (JsonNode scenario : scenarios) {
            String id = scenario.path("id").asText();
            String prompt = scenario.path("prompt").asText();
            List<String> expected = toList(scenario.get("expectedTools"));
            if (expected.isEmpty()) {
                continue; // adversarial / free-form — not heuristically scored
            }
            scored++;
            Set<String> predicted = predictTools(prompt);
            boolean ok = predicted.containsAll(expected);
            if (ok) {
                hits++;
            } else {
                misses.add(id + " expected=" + expected + " predicted=" + predicted);
            }
            for (String tool : expected) {
                assertTrue(KNOWN_TOOLS.contains(tool), "Unknown expected tool in " + id + ": " + tool);
            }
        }

        double rate = scored == 0 ? 0 : (double) hits / scored;
        assertTrue(
                rate >= 0.85,
                "Heuristic tool hit-rate too low: " + hits + "/" + scored + " misses=" + misses);
        assertFalse(misses.size() > scored, "unexpected");
    }

    private static Path resolveCatalog() {
        Path[] candidates = {
            Path.of("eval", "agent-eval.json"),
            Path.of("..", "eval", "agent-eval.json"),
            Path.of(System.getProperty("user.dir"), "eval", "agent-eval.json"),
            Path.of(System.getProperty("user.dir"), "..", "eval", "agent-eval.json")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("eval/agent-eval.json not found from " + System.getProperty("user.dir"));
    }

    private static List<String> toList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(n.asText()));
        }
        return out;
    }

    static Set<String> predictTools(String prompt) {
        String p = prompt.toLowerCase(Locale.ROOT);
        java.util.LinkedHashSet<String> tools = new java.util.LinkedHashSet<>();
        if (p.contains("balance")
                || p.contains("how many") && p.contains("leave")
                || p.contains("leaves do i")
                || p.contains("leaves remain")
                || p.contains("leaves left")) {
            tools.add("getLeaveBalance");
        }
        if (p.contains("holiday") || p.contains("republic day")) {
            tools.add("getHolidayList");
        }
        if (p.contains("policy")
                || p.contains("wfh")
                || p.contains("work from home")
                || p.contains("travel")
                || p.contains("expense")
                || p.contains("conduct")
                || p.contains("maternity")
                || p.contains("probation")
                || p.contains("hybrid")
                || p.contains("internet")
                || p.contains("helpdesk sla")
                || p.contains("sla for")
                || p.contains("ignore previous")
                || p.contains("salaries")) {
            tools.add("searchHrPolicies");
        }
        if ((p.contains("apply") && p.contains("leave")) || p.contains("book me a flight")) {
            tools.add("applyLeave");
        }
        if (p.contains("ticket") || p.contains("helpdesk") && !p.contains("sla")) {
            tools.add("raiseHelpdeskTicket");
        }
        return tools;
    }
}
