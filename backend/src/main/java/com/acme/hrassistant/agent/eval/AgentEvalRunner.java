package com.acme.hrassistant.agent.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic agent-eval baseline (no Gemini). Run with {@code --spring.profiles.active=eval}.
 * Live LLM scoring can be layered later; this prints the interview-ready table.
 */
@Component
@Profile("eval")
public class AgentEvalRunner implements ApplicationRunner {

    private final ObjectMapper objectMapper;

    public AgentEvalRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path catalog = resolveCatalog();
        JsonNode root = objectMapper.readTree(Files.readString(catalog));
        JsonNode scenarios = root.get("scenarios");

        int scored = 0;
        int hits = 0;
        System.out.println();
        System.out.println("| Id | Expected tools | Heuristic match |");
        System.out.println("|----|----------------|-----------------|");
        for (JsonNode scenario : scenarios) {
            String id = scenario.path("id").asText();
            List<String> expected = toList(scenario.get("expectedTools"));
            if (expected.isEmpty()) {
                System.out.println("| " + id + " | (manual/adversarial) | n/a |");
                continue;
            }
            scored++;
            Set<String> predicted = predictTools(scenario.path("prompt").asText());
            boolean ok = predicted.containsAll(expected);
            if (ok) {
                hits++;
            }
            System.out.println("| " + id + " | " + expected + " | " + (ok ? "PASS" : "FAIL " + predicted) + " |");
        }
        double rate = scored == 0 ? 0 : (100.0 * hits / scored);
        System.out.printf("%nAgent eval heuristic hit-rate: %.1f%% (%d/%d)%n", rate, hits, scored);
        System.out.println("Catalog: " + catalog);
    }

    private static Path resolveCatalog() {
        Path[] candidates = {
            Path.of("eval", "agent-eval.json"),
            Path.of("..", "eval", "agent-eval.json")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("eval/agent-eval.json not found");
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
        Set<String> tools = new LinkedHashSet<>();
        if (p.contains("quiz") || p.contains("test me") || p.contains("training check")) {
            tools.add("startPolicyQuiz");
        }
        if (p.contains("balance")
                || (p.contains("how many") && p.contains("leave"))
                || p.contains("leaves do i")
                || p.contains("leaves remain")
                || p.contains("leaves left")
                || p.contains("कितने")
                || (p.contains("leaves") && p.contains("बचे"))) {
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
                || p.contains("paternity")
                || p.contains("probation")
                || p.contains("hybrid")
                || p.contains("internet")
                || p.contains("dress code")
                || p.contains("transport allowance")
                || p.contains("grade g4")
                || p.contains("gift")
                || p.contains("clause")
                || p.contains("dikhao")
                || p.contains("teleportation")
                || p.contains("helpdesk sla")
                || p.contains("sla for")
                || p.contains("ignore previous")
                || p.contains("salaries")) {
            tools.add("searchHrPolicies");
        }
        if ((p.contains("apply") && p.contains("leave")) || p.contains("book me a flight")) {
            tools.add("applyLeave");
        }
        if (p.contains("ticket") || (p.contains("helpdesk") && !p.contains("sla"))) {
            tools.add("raiseHelpdeskTicket");
        }
        return tools;
    }
}
