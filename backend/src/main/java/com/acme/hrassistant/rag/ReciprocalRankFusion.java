package com.acme.hrassistant.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/**
 * Reciprocal rank fusion: score(doc) = sum over ranked lists of 1 / (k + rank), rank starting at 1.
 * Rank-based, so vector similarity and full-text scores never need to be normalised against each other.
 */
public final class ReciprocalRankFusion {

    private ReciprocalRankFusion() {
    }

    /**
     * Merges ranked lists (best first) into one list ordered by fused score. Documents are matched by
     * {@link Document#getId()}; the first instance seen is kept (put the vector list first so its
     * metadata wins). Ties keep first-seen order.
     */
    public static List<Document> fuse(List<List<Document>> rankedLists, int k, int limit) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> documents = new LinkedHashMap<>();
        for (List<Document> list : rankedLists) {
            for (int i = 0; i < list.size(); i++) {
                Document doc = list.get(i);
                scores.merge(doc.getId(), 1.0 / (k + i + 1), Double::sum);
                documents.putIfAbsent(doc.getId(), doc);
            }
        }
        List<Map.Entry<String, Double>> ordered = new ArrayList<>(scores.entrySet());
        ordered.sort(Map.Entry.<String, Double>comparingByValue().reversed()); // stable: ties keep insertion order
        return ordered.stream()
                .limit(Math.max(limit, 0))
                .map(entry -> documents.get(entry.getKey()))
                .toList();
    }
}
