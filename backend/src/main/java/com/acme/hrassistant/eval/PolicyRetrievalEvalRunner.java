package com.acme.hrassistant.eval;

import com.acme.hrassistant.eval.RetrievalEvalDataset.Question;
import com.acme.hrassistant.eval.RetrievalScorer.Aggregate;
import com.acme.hrassistant.eval.RetrievalScorer.QuestionScore;
import com.acme.hrassistant.rag.PolicyMetadataKeys;
import com.acme.hrassistant.rag.PolicySearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Retrieval eval: runs {@code eval/retrieval-eval.json} through the production
 * {@link PolicySearchService} (pgvector, and full-text + RRF for the hybrid column) and reports
 * recall@3, recall@5, hit@5 and MRR. Run with profile {@code eval-retrieval,dev}. Needs Postgres with the
 * seeded policy library indexed and a working embedding key; it never calls the chat model.
 */
@Component
@Profile("eval-retrieval")
public class PolicyRetrievalEvalRunner implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final PolicySearchService policySearchService;
    private final String embeddingModel;
    private final long delayMs;

    public PolicyRetrievalEvalRunner(
            ObjectMapper objectMapper,
            PolicySearchService policySearchService,
            @Value("${spring.ai.google.genai.embedding.text.options.model:unknown}") String embeddingModel,
            @Value("${app.eval.delay-ms:1000}") long delayMs) {
        this.objectMapper = objectMapper;
        this.policySearchService = policySearchService;
        this.embeddingModel = embeddingModel;
        this.delayMs = delayMs;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Question> questions = RetrievalEvalDataset.load(objectMapper);

        List<List<String>> vectorRetrieved = new ArrayList<>();
        List<List<String>> hybridRetrieved = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            System.out.printf("[%d/%d] %s ...%n", i + 1, questions.size(), q.id());
            vectorRetrieved.add(retrieve(q, false));
            hybridRetrieved.add(retrieve(q, true));
            if (delayMs > 0 && i + 1 < questions.size()) {
                Thread.sleep(delayMs); // embedding calls are rate limited on the free tier
            }
        }

        String report = render(questions, vectorRetrieved, hybridRetrieved);
        System.out.println(report);
        Path file = EvalFiles.resultsFile("retrieval-results.md");
        EvalFiles.write(file, report);
        System.out.println("Wrote " + file);
    }

    private List<String> retrieve(Question q, boolean hybrid) {
        PolicySearchService.SearchResult result =
                policySearchService.search(q.question(), q.employeeGrade(), q.employeeLocation(), hybrid);
        return result.documents().stream().map(PolicyRetrievalEvalRunner::label).toList();
    }

    /** Stable section label {@code <sourceFile>#<section>}; matches the labels in the dataset. */
    static String label(Document doc) {
        String file = Objects.toString(doc.getMetadata().get(PolicyMetadataKeys.SOURCE_FILE), "").trim();
        String section = Objects.toString(doc.getMetadata().get(PolicyMetadataKeys.SECTION), "").trim();
        return file + "#" + section;
    }

    private String render(List<Question> questions, List<List<String>> vector, List<List<String>> hybrid) {
        List<QuestionScore> vectorScores = new ArrayList<>();
        List<QuestionScore> hybridScores = new ArrayList<>();

        StringBuilder rows = new StringBuilder();
        rows.append("| Id | Relevant section(s) | Vector top results | Vector RR | Hybrid RR |\n");
        rows.append("|----|---------------------|--------------------|-----------|-----------|\n");
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            QuestionScore v = RetrievalScorer.score(vector.get(i), q.relevantDocSections());
            QuestionScore h = RetrievalScorer.score(hybrid.get(i), q.relevantDocSections());
            vectorScores.add(v);
            hybridScores.add(h);
            rows.append("| ").append(q.id())
                    .append(" | ").append(EvalFiles.markdownCell(String.join("; ", q.relevantDocSections())))
                    .append(" | ").append(EvalFiles.markdownCell(String.join("; ", vector.get(i).stream().distinct().toList())))
                    .append(" | ").append(String.format("%.2f", v.reciprocalRank()))
                    .append(" | ").append(String.format("%.2f", h.reciprocalRank()))
                    .append(" |\n");
        }

        Aggregate v = RetrievalScorer.aggregate(vectorScores);
        Aggregate h = RetrievalScorer.aggregate(hybridScores);

        StringBuilder out = new StringBuilder();
        out.append("# Policy retrieval eval\n\n");
        out.append("- Embedding model: `").append(embeddingModel).append("`\n");
        out.append("- Date: ").append(LocalDate.now()).append('\n');
        out.append("- Dataset: `eval/retrieval-eval.json` (").append(questions.size()).append(" questions)\n");
        out.append("- Labels: `<sourceFile>#<section heading>`; metrics over ranked, de-duplicated sections\n\n");
        out.append("| Mode | Recall@3 | Recall@5 | Hit@5 | MRR |\n");
        out.append("|------|----------|----------|-------|-----|\n");
        appendAggregate(out, "Vector (pgvector)", v);
        appendAggregate(out, "Hybrid (FTS + vector, RRF)", h);
        out.append("\n### Per question\n\n").append(rows);
        return out.toString();
    }

    private static void appendAggregate(StringBuilder out, String mode, Aggregate a) {
        out.append(String.format("| %s | %.3f | %.3f | %.3f | %.3f |%n",
                mode, a.recallAt3(), a.recallAt5(), a.hitAt5(), a.mrr()));
    }
}
