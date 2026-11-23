package com.acme.hrassistant.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dataset loader and deterministic checks for {@code eval/answer-quality-eval.json}.
 * The checks are intentionally simple string rules; an optional LLM judge (see
 * {@link AnswerQualityEvalRunner}) covers what string matching cannot.
 */
public final class AnswerQualityEval {

    public static final String FILE_NAME = "answer-quality-eval.json";

    private static final List<String> REFUSAL_MARKERS = List.of(
            "could not find",
            "couldn't find",
            "can't find",
            "cannot find",
            "unable to find",
            "not found",
            "no indexed",
            "not in the indexed",
            "not covered",
            "no information",
            "no relevant",
            "don't have",
            "do not have",
            "can't help",
            "cannot help",
            "can't share",
            "cannot share",
            "unable to");

    private AnswerQualityEval() {
    }

    public enum Type {
        ANSWERABLE,
        NOT_IN_POLICY,
        ADVERSARIAL;

        static Type parse(String raw) {
            return Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }

        public boolean expectsRefusal() {
            return this != ANSWERABLE;
        }
    }

    public record Item(
            String id,
            Type type,
            String username,
            String question,
            List<String> expectedCitations,
            List<String> requiredFacts,
            List<String> forbiddenClaims) {
    }

    /** Null check fields mean "not applicable for this item type". */
    public record Outcome(
            Boolean citationsOk,
            Boolean factsOk,
            Boolean refusalOk,
            boolean forbiddenClaimFree) {

        public boolean passed() {
            return forbiddenClaimFree
                    && (citationsOk == null || citationsOk)
                    && (factsOk == null || factsOk)
                    && (refusalOk == null || refusalOk);
        }
    }

    public static Path path() {
        return EvalFiles.resolve(FILE_NAME);
    }

    public static List<Item> load(ObjectMapper objectMapper) throws IOException {
        return load(objectMapper, path());
    }

    public static List<Item> load(ObjectMapper objectMapper, Path file) throws IOException {
        JsonNode root = objectMapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
        JsonNode items = root.get("items");
        if (items == null || !items.isArray()) {
            throw new IllegalStateException("'items' array missing in " + file);
        }
        List<Item> out = new ArrayList<>();
        for (JsonNode node : items) {
            out.add(new Item(
                    node.path("id").asText(),
                    Type.parse(node.path("type").asText()),
                    node.path("username").asText("priya.employee"),
                    node.path("question").asText(),
                    toList(node.get("expectedCitations")),
                    toList(node.get("requiredFacts")),
                    toList(node.get("forbiddenClaims"))));
        }
        return out;
    }

    /**
     * @param citationLabels labels of the citations returned to the UI, formatted {@code policyName#section}
     * @param helpdeskEscalated whether the reply staged a helpdesk pending action (automatic miss escalation)
     */
    public static Outcome score(Item item, String reply, List<String> citationLabels, boolean helpdeskEscalated) {
        String text = reply == null ? "" : reply.toLowerCase(Locale.ROOT);
        boolean forbiddenFree = item.forbiddenClaims().stream()
                .noneMatch(claim -> text.contains(claim.toLowerCase(Locale.ROOT)));

        if (item.type().expectsRefusal()) {
            boolean refused = helpdeskEscalated || looksLikeRefusal(text);
            return new Outcome(null, null, refused, forbiddenFree);
        }

        List<String> lowerLabels = citationLabels.stream().map(l -> l.toLowerCase(Locale.ROOT)).toList();
        boolean citationsOk = item.expectedCitations().stream()
                .allMatch(expected -> lowerLabels.contains(expected.toLowerCase(Locale.ROOT)));
        boolean factsOk = item.requiredFacts().stream()
                .allMatch(fact -> text.contains(fact.toLowerCase(Locale.ROOT)));
        return new Outcome(citationsOk, factsOk, null, forbiddenFree);
    }

    public static boolean looksLikeRefusal(String lowerCaseReply) {
        String text = lowerCaseReply == null ? "" : lowerCaseReply.toLowerCase(Locale.ROOT);
        return REFUSAL_MARKERS.stream().anyMatch(text::contains);
    }

    private static List<String> toList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
