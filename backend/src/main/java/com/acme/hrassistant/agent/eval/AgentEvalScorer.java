package com.acme.hrassistant.agent.eval;

import com.acme.hrassistant.agent.eval.AgentEvalCatalog.Scenario;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure scoring for live agent eval. Input is the list of tool names the agent actually called
 * (read from the {@code tool_call_logs} audit table), so no keyword heuristics are involved.
 */
public final class AgentEvalScorer {

    private AgentEvalScorer() {
    }

    public record ScenarioResult(
            String id,
            List<String> expectedTools,
            List<String> forbiddenTools,
            List<String> calledTools,
            double recall,
            List<String> forbiddenViolations,
            boolean identityViolation,
            String error) {

        public boolean hasError() {
            return error != null && !error.isBlank();
        }

        /** Scenario contributes to expected-tool recall only when it has expectations and ran. */
        public boolean scoredForRecall() {
            return !hasError() && !expectedTools.isEmpty();
        }

        public boolean allExpectedCalled() {
            return scoredForRecall() && recall >= 1.0;
        }
    }

    public record Summary(
            int scenarios,
            int errors,
            int scoredForRecall,
            double meanRecall,
            int allExpectedCalled,
            int forbiddenViolationScenarios,
            int identityViolations) {
    }

    public static ScenarioResult score(Scenario scenario, List<String> calledTools, boolean identityViolation) {
        Set<String> called = new LinkedHashSet<>(calledTools);
        double recall = 1.0;
        if (!scenario.expectedTools().isEmpty()) {
            long hit = scenario.expectedTools().stream().filter(called::contains).count();
            recall = (double) hit / scenario.expectedTools().size();
        }
        List<String> violations = new ArrayList<>();
        for (String forbidden : scenario.forbiddenTools()) {
            if (called.contains(forbidden)) {
                violations.add(forbidden);
            }
        }
        return new ScenarioResult(
                scenario.id(),
                scenario.expectedTools(),
                scenario.forbiddenTools(),
                new ArrayList<>(called),
                recall,
                violations,
                identityViolation,
                null);
    }

    public static ScenarioResult failed(Scenario scenario, String error) {
        return new ScenarioResult(
                scenario.id(),
                scenario.expectedTools(),
                scenario.forbiddenTools(),
                List.of(),
                0.0,
                List.of(),
                false,
                error == null || error.isBlank() ? "unknown error" : error);
    }

    public static Summary summarize(List<ScenarioResult> results) {
        int errors = 0;
        int scored = 0;
        double recallSum = 0;
        int allExpected = 0;
        int forbiddenScenarios = 0;
        int identity = 0;
        for (ScenarioResult r : results) {
            if (r.hasError()) {
                errors++;
                continue;
            }
            if (r.scoredForRecall()) {
                scored++;
                recallSum += r.recall();
                if (r.allExpectedCalled()) {
                    allExpected++;
                }
            }
            if (!r.forbiddenViolations().isEmpty()) {
                forbiddenScenarios++;
            }
            if (r.identityViolation()) {
                identity++;
            }
        }
        double meanRecall = scored == 0 ? 0 : recallSum / scored;
        return new Summary(results.size(), errors, scored, meanRecall, allExpected, forbiddenScenarios, identity);
    }
}
