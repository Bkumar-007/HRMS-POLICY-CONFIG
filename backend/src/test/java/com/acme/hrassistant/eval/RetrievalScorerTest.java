package com.acme.hrassistant.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acme.hrassistant.eval.RetrievalScorer.Aggregate;
import com.acme.hrassistant.eval.RetrievalScorer.QuestionScore;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RetrievalScorerTest {

    @Test
    void recallAtKCountsRelevantItemsInTopK() {
        List<String> retrieved = List.of("a", "b", "c", "d", "e", "f");
        Set<String> relevant = Set.of("b", "f");

        assertEquals(0.5, RetrievalScorer.recallAtK(retrieved, relevant, 5), 1e-9);
        assertEquals(1.0, RetrievalScorer.recallAtK(retrieved, relevant, 6), 1e-9);
        assertEquals(0.0, RetrievalScorer.recallAtK(retrieved, relevant, 1), 1e-9);
    }

    @Test
    void duplicateSectionsAreCollapsedBeforeRanking() {
        // Two chunks of section "a" must not push "b" down to rank 3.
        List<String> retrieved = List.of("a", "a", "b");
        assertEquals(0.5, RetrievalScorer.reciprocalRank(retrieved, Set.of("b")), 1e-9);
        assertEquals(1.0, RetrievalScorer.recallAtK(retrieved, Set.of("b"), 2), 1e-9);
    }

    @Test
    void reciprocalRankUsesFirstRelevantHitAndIsZeroWhenMissing() {
        assertEquals(1.0 / 3, RetrievalScorer.reciprocalRank(List.of("x", "y", "z"), Set.of("z", "q")), 1e-9);
        assertEquals(0.0, RetrievalScorer.reciprocalRank(List.of("x", "y"), Set.of("z")), 1e-9);
        assertEquals(0.0, RetrievalScorer.reciprocalRank(List.of(), Set.of("z")), 1e-9);
    }

    @Test
    void aggregateAveragesPerQuestionScores() {
        QuestionScore hit = RetrievalScorer.score(List.of("a"), Set.of("a"));
        QuestionScore miss = RetrievalScorer.score(List.of("x"), Set.of("a"));

        Aggregate aggregate = RetrievalScorer.aggregate(List.of(hit, miss));

        assertEquals(2, aggregate.questions());
        assertEquals(0.5, aggregate.recallAt5(), 1e-9);
        assertEquals(0.5, aggregate.mrr(), 1e-9);
        assertEquals(0.5, aggregate.hitAt5(), 1e-9);
    }

    @Test
    void emptyRelevantSetScoresZeroInsteadOfDividingByZero() {
        assertEquals(0.0, RetrievalScorer.recallAtK(List.of("a"), Set.of(), 5), 1e-9);
    }
}
