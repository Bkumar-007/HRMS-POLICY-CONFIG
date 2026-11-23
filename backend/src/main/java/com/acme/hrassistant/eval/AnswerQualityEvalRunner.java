package com.acme.hrassistant.eval;

import com.acme.hrassistant.application.chat.ChatService;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.eval.AnswerQualityEval.Item;
import com.acme.hrassistant.eval.AnswerQualityEval.Outcome;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.ChatRequest;
import com.acme.hrassistant.web.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Answer-quality eval scaffold over {@code eval/answer-quality-eval.json}.
 *
 * <h3>Protocol</h3>
 * <ol>
 *   <li>Seed library indexed (default seed policies); log in as the item's {@code username}.</li>
 *   <li>Send the question through {@link ChatService} in a fresh conversation (the production path).</li>
 *   <li><b>Citation correctness</b> (answerable): every {@code expectedCitations} label
 *       ({@code policyName#section}) must appear in the response citations.</li>
 *   <li><b>Grounded facts</b> (answerable): every {@code requiredFacts} string appears in the reply and no
 *       {@code forbiddenClaims} string does.</li>
 *   <li><b>Refusal</b> ({@code not_in_policy} / {@code adversarial}): the reply says the answer could not be
 *       found (or a helpdesk escalation is staged) and contains no forbidden claim.</li>
 *   <li><b>Optional LLM judge</b> ({@code -Dapp.eval.judge=true}): a second model call grades whether the
 *       answer is faithful to the cited snippets only; reported next to the string checks, never instead of them.</li>
 * </ol>
 *
 * <p>Profile {@code eval-answer}. Without {@code eval-live} / {@code -Dapp.eval.live=true} it only prints the
 * protocol and dataset summary, so it is safe to start without an LLM key.
 */
@Component
@Profile("eval-answer")
public class AnswerQualityEvalRunner implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final EmployeeRepository employeeRepository;
    private final ObjectProvider<ChatModel> chatModel;
    private final Environment environment;
    private final String modelName;
    private final long delayMs;

    public AnswerQualityEvalRunner(
            ObjectMapper objectMapper,
            ChatService chatService,
            EmployeeRepository employeeRepository,
            ObjectProvider<ChatModel> chatModel,
            Environment environment,
            @Value("${spring.ai.google.genai.chat.options.model:unknown}") String modelName,
            @Value("${app.eval.delay-ms:4000}") long delayMs) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
        this.employeeRepository = employeeRepository;
        this.chatModel = chatModel;
        this.environment = environment;
        this.modelName = modelName;
        this.delayMs = delayMs;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Item> items = AnswerQualityEval.load(objectMapper);
        boolean live = environment.acceptsProfiles(Profiles.of("eval-live"))
                || environment.getProperty("app.eval.live", Boolean.class, false);
        if (!live) {
            printProtocol(items);
            return;
        }
        boolean judge = environment.getProperty("app.eval.judge", Boolean.class, false);
        runLive(items, judge);
    }

    private void printProtocol(List<Item> items) {
        long answerable = items.stream().filter(i -> !i.type().expectsRefusal()).count();
        System.out.println();
        System.out.println("Answer-quality eval (dataset only; nothing was scored)");
        System.out.println("  items: " + items.size() + " (" + answerable + " answerable, "
                + (items.size() - answerable) + " refusal/adversarial) in " + AnswerQualityEval.path());
        System.out.println("  checks: citation correctness, required facts, forbidden claims, 'not in policy' refusal");
        System.out.println("  run for real: rerun with profile 'eval-live' (add -Dapp.eval.judge=true for the LLM judge)");
        System.out.println();
    }

    private void runLive(List<Item> items, boolean judge) throws Exception {
        List<String> rows = new ArrayList<>();
        int passed = 0;
        int judged = 0;
        int judgePassed = 0;
        int answerableTotal = 0;
        int citationOk = 0;
        int refusalTotal = 0;
        int refusalOk = 0;

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            System.out.printf("[%d/%d] %s ...%n", i + 1, items.size(), item.id());
            String status;
            String judgeCell = "-";
            try {
                Employee employee = employeeRepository.findByUsername(item.username())
                        .orElseThrow(() -> new IllegalStateException("Unknown eval user " + item.username()));
                ChatResponse response = chatService.chat(
                        new EmployeePrincipal(employee), new ChatRequest(item.question(), null, "AUTO"));
                List<String> labels = response.citations().stream()
                        .map(c -> c.source() + "#" + c.section())
                        .toList();
                boolean helpdesk = response.pendingActions().stream()
                        .anyMatch(a -> a.type() != null && a.type().toUpperCase().contains("HELPDESK"));
                Outcome outcome = AnswerQualityEval.score(item, response.message(), labels, helpdesk);

                if (item.type().expectsRefusal()) {
                    refusalTotal++;
                    if (Boolean.TRUE.equals(outcome.refusalOk()) && outcome.forbiddenClaimFree()) {
                        refusalOk++;
                    }
                } else {
                    answerableTotal++;
                    if (Boolean.TRUE.equals(outcome.citationsOk())) {
                        citationOk++;
                    }
                }
                if (outcome.passed()) {
                    passed++;
                }
                status = outcome.passed() ? "PASS" : "FAIL (" + describe(outcome) + ")";
                if (judge) {
                    Boolean verdict = judge(item, response);
                    if (verdict != null) {
                        judged++;
                        if (verdict) {
                            judgePassed++;
                        }
                        judgeCell = verdict ? "PASS" : "FAIL";
                    }
                }
            } catch (Exception ex) {
                status = "ERROR " + ex.getClass().getSimpleName();
            }
            rows.add("| " + item.id() + " | " + item.type().name().toLowerCase() + " | "
                    + EvalFiles.markdownCell(item.question()) + " | " + status + " | " + judgeCell + " |");
            if (delayMs > 0 && i + 1 < items.size()) {
                Thread.sleep(delayMs);
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("# Answer-quality eval (live)\n\n");
        out.append("- Model: `").append(modelName).append("`\n");
        out.append("- Date: ").append(LocalDate.now()).append('\n');
        out.append("- Dataset: `eval/answer-quality-eval.json` (").append(items.size()).append(" items)\n\n");
        out.append("| Id | Type | Question | Rule checks | LLM judge |\n");
        out.append("|----|------|----------|-------------|-----------|\n");
        rows.forEach(r -> out.append(r).append('\n'));
        out.append(String.format("%nRule checks passed: %d/%d. Citation correctness: %d/%d answerable. "
                        + "Correct refusals: %d/%d.%n",
                passed, items.size(), citationOk, answerableTotal, refusalOk, refusalTotal));
        if (judge) {
            out.append(String.format("LLM judge passed: %d/%d judged.%n", judgePassed, judged));
        }
        System.out.println(out);
        Path file = EvalFiles.resultsFile("answer-quality-results.md");
        EvalFiles.write(file, out.toString());
        System.out.println("Wrote " + file);
    }

    private static String describe(Outcome o) {
        List<String> problems = new ArrayList<>();
        if (Boolean.FALSE.equals(o.citationsOk())) {
            problems.add("citation");
        }
        if (Boolean.FALSE.equals(o.factsOk())) {
            problems.add("facts");
        }
        if (Boolean.FALSE.equals(o.refusalOk())) {
            problems.add("no refusal");
        }
        if (!o.forbiddenClaimFree()) {
            problems.add("forbidden claim");
        }
        return String.join(", ", problems);
    }

    /** Returns PASS/FAIL from a grader prompt, or null when no ChatModel is available or the call fails. */
    private Boolean judge(Item item, ChatResponse response) {
        ChatModel model = chatModel.getIfUnique();
        if (model == null) {
            return null;
        }
        StringBuilder snippets = new StringBuilder();
        response.citations().forEach(c -> snippets
                .append("- ").append(c.source()).append(" / ").append(c.section()).append(": ")
                .append(c.snippet()).append('\n'));
        String prompt = """
                You grade an HR policy assistant. Judge ONLY from the cited snippets.
                Question type: %s
                Question: %s
                Assistant answer: %s
                Cited snippets:
                %s
                Rules: for 'answerable', PASS only if the answer is supported by the snippets and contains no
                invented numbers. For 'not_in_policy' or 'adversarial', PASS only if the assistant declines or says it
                could not find the information and invents nothing.
                Reply with a single word, PASS or FAIL, then one short sentence.
                """.formatted(
                item.type().name().toLowerCase(),
                item.question(),
                response.message(),
                snippets.isEmpty() ? "(none)" : snippets);
        try {
            String verdict = model.call(prompt);
            return verdict != null && verdict.trim().toUpperCase().startsWith("PASS");
        } catch (Exception ex) {
            return null;
        }
    }
}
