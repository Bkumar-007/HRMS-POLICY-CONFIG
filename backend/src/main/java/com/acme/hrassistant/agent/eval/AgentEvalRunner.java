package com.acme.hrassistant.agent.eval;

import com.acme.hrassistant.agent.eval.AgentEvalCatalog.Scenario;
import com.acme.hrassistant.agent.eval.AgentEvalScorer.ScenarioResult;
import com.acme.hrassistant.agent.eval.AgentEvalScorer.Summary;
import com.acme.hrassistant.application.chat.ChatService;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.ToolCallLog;
import com.acme.hrassistant.domain.ToolCallLogRepository;
import com.acme.hrassistant.eval.EvalFiles;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.ChatRequest;
import com.acme.hrassistant.web.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Agent tool-selection eval over {@code eval/agent-eval.json}.
 *
 * <ul>
 *   <li><b>Catalog mode</b> (profile {@code eval}): prints the scenario catalog only. It does not
 *       claim anything about agent quality.</li>
 *   <li><b>Live mode</b> ({@code eval-live} profile, or {@code eval} plus {@code -Dapp.eval.live=true}): sends every
 *       prompt through the real {@link ChatService} (same path as {@code POST /api/chat}), reads the
 *       tools the model actually called from the {@code tool_call_logs} audit table, and scores
 *       expected-tool recall and forbidden-tool violations. Needs Postgres and a working LLM key.</li>
 * </ul>
 */
@Component
@Profile("(eval | eval-live) & !eval-answer & !eval-retrieval")
public class AgentEvalRunner implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final EmployeeRepository employeeRepository;
    private final ToolCallLogRepository toolCallLogRepository;
    private final Environment environment;
    private final String modelName;
    private final String evalUsername;
    private final long delayMs;

    public AgentEvalRunner(
            ObjectMapper objectMapper,
            ChatService chatService,
            EmployeeRepository employeeRepository,
            ToolCallLogRepository toolCallLogRepository,
            Environment environment,
            @Value("${spring.ai.google.genai.chat.options.model:unknown}") String modelName,
            @Value("${app.eval.username:priya.employee}") String evalUsername,
            @Value("${app.eval.delay-ms:4000}") long delayMs) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
        this.employeeRepository = employeeRepository;
        this.toolCallLogRepository = toolCallLogRepository;
        this.environment = environment;
        this.modelName = modelName;
        this.evalUsername = evalUsername;
        this.delayMs = delayMs;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Scenario> scenarios = AgentEvalCatalog.load(objectMapper);
        boolean live = environment.acceptsProfiles(Profiles.of("eval-live"))
                || environment.getProperty("app.eval.live", Boolean.class, false);
        if (live) {
            runLive(scenarios);
        } else {
            printCatalog(scenarios);
        }
    }

    private void printCatalog(List<Scenario> scenarios) {
        StringBuilder out = new StringBuilder();
        out.append("\n| Id | Prompt | Expected tools | Forbidden tools |\n");
        out.append("|----|--------|----------------|-----------------|\n");
        for (Scenario s : scenarios) {
            out.append("| ").append(s.id())
                    .append(" | ").append(EvalFiles.markdownCell(s.prompt()))
                    .append(" | ").append(s.expectedTools().isEmpty() ? "(none)" : s.expectedTools())
                    .append(" | ").append(s.forbiddenTools().isEmpty() ? "(none)" : s.forbiddenTools())
                    .append(" |\n");
        }
        out.append("\nCatalog: ").append(scenarios.size()).append(" scenarios in ").append(AgentEvalCatalog.path()).append('\n');
        out.append("This is a catalog summary only; it does not measure agent quality.\n");
        out.append("To score the real agent, rerun with profile 'eval-live' (or -Dapp.eval.live=true).\n");
        System.out.println(out);
    }

    private void runLive(List<Scenario> scenarios) throws Exception {
        Employee employee = employeeRepository.findByUsername(evalUsername)
                .orElseThrow(() -> new IllegalStateException("Eval user not found: " + evalUsername));
        EmployeePrincipal principal = new EmployeePrincipal(employee);

        List<ScenarioResult> results = new ArrayList<>();
        for (int i = 0; i < scenarios.size(); i++) {
            Scenario scenario = scenarios.get(i);
            System.out.printf("[%d/%d] %s ...%n", i + 1, scenarios.size(), scenario.id());
            results.add(runScenario(principal, scenario));
            if (delayMs > 0 && i + 1 < scenarios.size()) {
                Thread.sleep(delayMs); // stay under free-tier LLM rate limits
            }
        }

        String report = renderReport(scenarios, results, employee.getUsername());
        System.out.println(report);
        Path file = EvalFiles.resultsFile("agent-eval-results.md");
        EvalFiles.write(file, report);
        System.out.println("Wrote " + file);
    }

    private ScenarioResult runScenario(EmployeePrincipal principal, Scenario scenario) {
        try {
            // New conversation per scenario so memory from earlier prompts cannot leak into tool choice.
            ChatResponse response = chatService.chat(principal, new ChatRequest(scenario.prompt(), null, "AUTO"));
            List<ToolCallLog> logs =
                    toolCallLogRepository.findByConversationIdOrderByIdAsc(response.conversationId());
            List<String> called = logs.stream().map(ToolCallLog::getToolName).toList();
            boolean identityViolation =
                    logs.stream().anyMatch(l -> !l.getEmployeeId().equals(principal.getEmployeeId()));
            return AgentEvalScorer.score(scenario, called, identityViolation);
        } catch (Exception ex) {
            return AgentEvalScorer.failed(scenario, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String renderReport(List<Scenario> scenarios, List<ScenarioResult> results, String username) {
        Summary summary = AgentEvalScorer.summarize(results);
        StringBuilder out = new StringBuilder();
        out.append("# Agent tool-selection eval (live)\n\n");
        out.append("- Model: `").append(modelName).append("`\n");
        out.append("- Date: ").append(LocalDate.now()).append('\n');
        out.append("- Employee: `").append(username).append("`\n");
        out.append("- Path: `ChatService.chat` -> real LLM tool calling; tools read from `tool_call_logs`\n\n");
        out.append("| Id | Expected | Called | Recall | Forbidden hit | Status |\n");
        out.append("|----|----------|--------|--------|---------------|--------|\n");
        for (int i = 0; i < results.size(); i++) {
            ScenarioResult r = results.get(i);
            Scenario s = scenarios.get(i);
            String status;
            if (r.hasError()) {
                status = "ERROR " + EvalFiles.markdownCell(r.error());
            } else if (r.identityViolation() || !r.forbiddenViolations().isEmpty()) {
                status = "FAIL";
            } else if (r.expectedTools().isEmpty()) {
                status = "n/a (no expected tools)";
            } else {
                status = r.allExpectedCalled() ? "PASS" : "MISS";
            }
            out.append("| ").append(s.id())
                    .append(" | ").append(r.expectedTools().isEmpty() ? "(none)" : r.expectedTools())
                    .append(" | ").append(r.calledTools().isEmpty() ? "(none)" : r.calledTools())
                    .append(" | ").append(r.scoredForRecall() ? String.format("%.2f", r.recall()) : "-")
                    .append(" | ").append(r.forbiddenViolations().isEmpty() ? "-" : r.forbiddenViolations())
                    .append(" | ").append(status)
                    .append(" |\n");
        }
        out.append('\n');
        out.append(String.format(
                "Expected-tool recall: %.1f%% mean over %d scenario(s); %d/%d called every expected tool.%n",
                100.0 * summary.meanRecall(), summary.scoredForRecall(), summary.allExpectedCalled(), summary.scoredForRecall()));
        out.append(String.format(
                "Forbidden-tool violations: %d scenario(s). Cross-employee tool calls: %d. Errors: %d of %d.%n",
                summary.forbiddenViolationScenarios(), summary.identityViolations(), summary.errors(), summary.scenarios()));
        return out.toString();
    }
}
