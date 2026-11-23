package com.acme.hrassistant.agent.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.hrassistant.agent.eval.AgentEvalCatalog.Scenario;
import com.acme.hrassistant.agent.eval.AgentEvalScorer.ScenarioResult;
import com.acme.hrassistant.agent.eval.AgentEvalScorer.Summary;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentEvalScorerTest {

    @Test
    void recallIsFractionOfExpectedToolsActuallyCalled() {
        Scenario s = new Scenario("T1", "p", List.of("getLeaveBalance", "applyLeave"), List.of(), "");
        ScenarioResult r = AgentEvalScorer.score(s, List.of("getLeaveBalance"), false);
        assertEquals(0.5, r.recall(), 1e-9);
        assertFalse(r.allExpectedCalled());
    }

    @Test
    void forbiddenToolCallIsReported() {
        Scenario s = new Scenario("T2", "p", List.of("searchHrPolicies"), List.of("applyLeave"), "");
        ScenarioResult r = AgentEvalScorer.score(s, List.of("searchHrPolicies", "applyLeave"), false);
        assertEquals(List.of("applyLeave"), r.forbiddenViolations());
        assertTrue(r.allExpectedCalled());
    }

    @Test
    void summaryExcludesErrorsAndScenariosWithoutExpectations() {
        Scenario ok = new Scenario("A", "p", List.of("getHolidayList"), List.of(), "");
        Scenario adversarial = new Scenario("B", "p", List.of(), List.of("applyLeave"), "");
        Scenario broken = new Scenario("C", "p", List.of("searchHrPolicies"), List.of(), "");

        Summary summary = AgentEvalScorer.summarize(List.of(
                AgentEvalScorer.score(ok, List.of("getHolidayList"), false),
                AgentEvalScorer.score(adversarial, List.of("applyLeave"), true),
                AgentEvalScorer.failed(broken, "timeout")));

        assertEquals(3, summary.scenarios());
        assertEquals(1, summary.errors());
        assertEquals(1, summary.scoredForRecall());
        assertEquals(1.0, summary.meanRecall(), 1e-9);
        assertEquals(1, summary.forbiddenViolationScenarios());
        assertEquals(1, summary.identityViolations());
    }
}
