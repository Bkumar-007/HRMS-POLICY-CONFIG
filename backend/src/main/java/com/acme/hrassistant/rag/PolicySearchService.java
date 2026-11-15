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
    private final int topK;
    private final double similarityThreshold;

    public PolicySearchService(VectorStore vectorStore, AppProperties appProperties) {
        this.vectorStore = vectorStore;
        this.topK = appProperties.getRag().getTopK();
        this.similarityThreshold = appProperties.getRag().getSimilarityThreshold();
    }

    public SearchResult search(String query, String employeeGrade, String employeeLocation) {
        List<Document> raw = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(Math.max(topK * 3, topK))
                .similarityThreshold(similarityThreshold)
                .build());

        List<Document> filtered = raw.stream()
                .filter(doc -> matchesAttribute(doc, PolicyMetadataKeys.GRADE, employeeGrade))
                .filter(doc -> matchesAttribute(doc, PolicyMetadataKeys.LOCATION, employeeLocation))
                .limit(topK)
                .toList();

        List<CitationDto> citations = new ArrayList<>();
        Map<String, CitationDto> dedupe = new LinkedHashMap<>();
        StringBuilder context = new StringBuilder();

        for (Document doc : filtered) {
            Map<String, Object> meta = doc.getMetadata();
            String source = str(meta.get(PolicyMetadataKeys.POLICY_NAME));
            String section = str(meta.get(PolicyMetadataKeys.SECTION));
            String snippet = snippet(doc.getText());
            String key = source + "|" + section;
            dedupe.putIfAbsent(key, new CitationDto(
                    source.isBlank() ? "Unknown policy" : source,
                    section.isBlank() ? "General" : section,
                    snippet));

            context.append("---\n")
                    .append("Policy: ").append(source).append('\n')
                    .append("Section: ").append(section).append('\n')
                    .append(doc.getText()).append("\n\n");
        }
        citations.addAll(dedupe.values());

        return new SearchResult(filtered, citations, context.toString().trim());
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
