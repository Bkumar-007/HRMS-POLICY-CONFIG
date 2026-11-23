package com.acme.hrassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class ReciprocalRankFusionTest {

    private static Document doc(String id) {
        return new Document(id, "text " + id, Map.of());
    }

    private static List<String> ids(List<Document> docs) {
        return docs.stream().map(Document::getId).toList();
    }

    @Test
    void documentRankedWellByBothListsBeatsDocumentTopInOnlyOne() {
        List<Document> vector = List.of(doc("a"), doc("b"), doc("c"));
        List<Document> keyword = List.of(doc("b"), doc("d"), doc("a"));

        // b: 1/62 + 1/61, a: 1/61 + 1/63, c: 1/63, d: 1/62
        List<Document> fused = ReciprocalRankFusion.fuse(List.of(vector, keyword), 60, 10);

        assertEquals(List.of("b", "a", "d", "c"), ids(fused));
    }

    @Test
    void keywordOnlyHitsAreAddedAndLimitIsApplied() {
        List<Document> fused = ReciprocalRankFusion.fuse(
                List.of(List.of(doc("a")), List.of(doc("z"))), 60, 2);

        assertEquals(List.of("a", "z"), ids(fused)); // equal scores keep first-seen order
        assertEquals(1, ReciprocalRankFusion.fuse(List.of(List.of(doc("a")), List.of(doc("z"))), 60, 1).size());
    }

    @Test
    void emptyInputsYieldEmptyResult() {
        assertEquals(List.of(), ReciprocalRankFusion.fuse(List.of(List.of(), List.of()), 60, 5));
    }
}
