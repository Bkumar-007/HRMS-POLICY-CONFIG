package com.acme.hrassistant.eval;

import java.util.List;
import java.util.Set;

/**
 * Pure retrieval metrics. The retrieved list is ranked best-first; duplicates (several chunks of
 * the same section) are collapsed to their first rank before scoring.
 */
public final class RetrievalScorer {

    private RetrievalScorer() {
    }

    /** Fraction of the relevant items found in the top-k distinct retrieved items. */
    public static double recallAtK(List<String> retrieved, Set<String> relevant, int k) {
        if (relevant.isEmpty() || k <= 0) {
            return 0.0;
        }
        long found = retrieved.stream().distinct().limit(k).filter(relevant::contains).count();
        return (double) found / relevant.size();
    }

    /** 1.0 when at least one relevant item is in the top-k, else 0.0. */
    public static double hitAtK(List<String> retrieved, Set<String> relevant, int k) {
        return retrieved.stream().distinct().limit(k).anyMatch(relevant::contains) ? 1.0 : 0.0;
    }

    /** 1 / rank of the first relevant item (rank starts at 1), or 0 when none is retrieved. */
    public static double reciprocalRank(List<String> retrieved, Set<String> relevant) {
        List<String> distinct = retrieved.stream().distinct().toList();
        for (int i = 0; i < distinct.size(); i++) {
            if (relevant.contains(distinct.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    public record QuestionScore(double recallAt3, double recallAt5, double hitAt5, double reciprocalRank) {
    }

    public static QuestionScore score(List<String> retrieved, Set<String> relevant) {
        return new QuestionScore(
                recallAtK(retrieved, relevant, 3),
                recallAtK(retrieved, relevant, 5),
                hitAtK(retrieved, relevant, 5),
                reciprocalRank(retrieved, relevant));
    }

    public record Aggregate(int questions, double recallAt3, double recallAt5, double hitAt5, double mrr) {
    }

    public static Aggregate aggregate(List<QuestionScore> scores) {
        if (scores.isEmpty()) {
            return new Aggregate(0, 0, 0, 0, 0);
        }
        double r3 = 0;
        double r5 = 0;
        double h5 = 0;
        double rr = 0;
        for (QuestionScore s : scores) {
            r3 += s.recallAt3();
            r5 += s.recallAt5();
            h5 += s.hitAt5();
            rr += s.reciprocalRank();
        }
        int n = scores.size();
        return new Aggregate(n, r3 / n, r5 / n, h5 / n, rr / n);
    }
}
