package com.acme.hrassistant.rag;

import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.web.dto.ChatResponse.CitationDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class PolicySearchService {

    private final VectorStore vectorStore;
    private final PolicyKeywordSearch keywordSearch;
    private final int topK;
    private final double similarityThreshold;
    private final boolean hybridEnabled;
    private final int rrfK;
    private final int keywordCandidates;

    public PolicySearchService(
            VectorStore vectorStore, PolicyKeywordSearch keywordSearch, AppProperties appProperties) {
        this.vectorStore = vectorStore;
        this.keywordSearch = keywordSearch;
        this.topK = appProperties.getRag().getTopK();
        this.similarityThreshold = appProperties.getRag().getSimilarityThreshold();
        this.hybridEnabled = appProperties.getRag().getHybrid().isEnabled();
        this.rrfK = appProperties.getRag().getHybrid().getRrfK();
        this.keywordCandidates = appProperties.getRag().getHybrid().getCandidates();
    }

    /** Uses hybrid retrieval when {@code app.rag.hybrid.enabled=true}, vector-only otherwise. */
    public SearchResult search(String query, String employeeGrade, String employeeLocation) {
        return search(query, employeeGrade, employeeLocation, hybridEnabled);
    }

    /**
     * Hybrid mode merges pgvector similarity hits with Postgres full-text hits via reciprocal rank
     * fusion. Full-text hits are only added when the vector search found something above the
     * similarity threshold, so questions that are genuinely outside the policy library still return
     * nothing (and trigger the helpdesk escalation) instead of matching on a stray common word.
     */
    public SearchResult search(String query, String employeeGrade, String employeeLocation, boolean hybrid) {
        List<Document> raw = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(Math.max(topK * 3, topK))
                .similarityThreshold(similarityThreshold)
                .build());

        List<Document> vectorHits = filterByAttributes(raw, employeeGrade, employeeLocation);
        List<Document> filtered;
        if (hybrid && !vectorHits.isEmpty()) {
            List<Document> keywordHits = filterByAttributes(
                    keywordSearch.search(query, keywordCandidates), employeeGrade, employeeLocation);
            filtered = ReciprocalRankFusion.fuse(List.of(vectorHits, keywordHits), rrfK, topK);
        } else {
            filtered = vectorHits.stream().limit(topK).toList();
        }

        List<CitationDto> citations = new ArrayList<>();
        Map<String, CitationDto> dedupe = new LinkedHashMap<>();
        StringBuilder context = new StringBuilder();

        for (Document doc : filtered) {
            Map<String, Object> meta = doc.getMetadata();
            String policyId = str(meta.get(PolicyMetadataKeys.POLICY_ID));
            String source = str(meta.get(PolicyMetadataKeys.POLICY_NAME));
            String section = str(meta.get(PolicyMetadataKeys.SECTION));
            String snippet = snippet(doc.getText());
            String key = policyId + "|" + source + "|" + section;
            dedupe.putIfAbsent(key, new CitationDto(
                    policyId.isBlank() ? null : policyId,
                    source.isBlank() ? "Unknown policy" : source,
                    section.isBlank() ? "General" : section,
                    snippet));

            context.append("---\n")
                    .append("PolicyId: ").append(policyId).append('\n')
                    .append("Policy: ").append(source).append('\n')
                    .append("Section: ").append(section).append('\n')
                    .append(doc.getText()).append("\n\n");
        }
        citations.addAll(dedupe.values());

        return new SearchResult(filtered, citations, context.toString().trim());
    }

    private static List<Document> filterByAttributes(List<Document> docs, String employeeGrade, String employeeLocation) {
        return docs.stream()
                .filter(doc -> matchesAttribute(doc, PolicyMetadataKeys.GRADE, employeeGrade))
                .filter(doc -> matchesAttribute(doc, PolicyMetadataKeys.LOCATION, employeeLocation))
                .toList();
    }

    private static boolean matchesAttribute(Document doc, String key, String employeeValue) {
        Object raw = doc.getMetadata().get(key);
        if (raw == null) {
            return true; // unrestricted chunk
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty() || "*".equals(value) || "ALL".equalsIgnoreCase(value)) {
            return true;
        }
        if (employeeValue == null || employeeValue.isBlank()) {
            return true;
        }
        return value.equalsIgnoreCase(employeeValue.trim());
    }

    private static String str(Object value) {
        return value == null ? "" : Objects.toString(value, "").trim();
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 180 ? compact : compact.substring(0, 177) + "...";
    }

    public record SearchResult(List<Document> documents, List<CitationDto> citations, String contextForModel) {
    }
}
