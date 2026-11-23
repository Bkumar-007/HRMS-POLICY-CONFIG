package com.acme.hrassistant.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Postgres full-text search over the same {@code vector_store} table pgvector uses (default Spring AI
 * table name), so lexical hits carry the exact chunk ids and metadata of the vector hits.
 *
 * <p>The query is parsed with {@code plainto_tsquery} and its AND operators are rewritten to OR so a
 * natural-language question matches chunks containing any of its stemmed terms; {@code ts_rank_cd}
 * orders them. On a small corpus a sequential scan is fine; add
 * {@code CREATE INDEX ON vector_store USING gin (to_tsvector('english', content))} for larger libraries.
 */
@Component
public class PolicyKeywordSearch {

    private static final Logger log = LoggerFactory.getLogger(PolicyKeywordSearch.class);

    private static final String SQL = """
            WITH q AS (
              SELECT to_tsquery('english', replace(plainto_tsquery('english', ?)::text, '&', '|')) AS query
            )
            SELECT v.id::text AS id, v.content AS content, v.metadata::text AS metadata
            FROM vector_store v, q
            WHERE to_tsvector('english', v.content) @@ q.query
            ORDER BY ts_rank_cd(to_tsvector('english', v.content), q.query) DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PolicyKeywordSearch(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** Ranked chunks (best first). Returns an empty list on blank queries or any SQL failure. */
    public List<Document> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(SQL, (rs, rowNum) -> toDocument(
                    rs.getString("id"), rs.getString("content"), rs.getString("metadata")), query, limit);
        } catch (Exception ex) {
            log.warn("Full-text policy search failed; falling back to vector-only: {}", ex.getMessage());
            return List.of();
        }
    }

    private Document toDocument(String id, String content, String metadataJson) {
        Map<String, Object> metadata = new HashMap<>();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                metadata.putAll(objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
                }));
            } catch (Exception ex) {
                log.debug("Could not parse chunk metadata for {}: {}", id, ex.getMessage());
            }
        }
        return new Document(id, content == null ? "" : content, metadata);
    }
}
