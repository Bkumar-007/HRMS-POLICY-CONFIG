package com.acme.hrassistant.rag;

import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.domain.PolicyStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyChunker policyChunker;
    private final VectorStore vectorStore;
    private final LocalPolicyFileStore localPolicyFileStore;

    public PolicyIngestionService(
            PolicyDocumentRepository policyDocumentRepository,
            PolicyChunker policyChunker,
            VectorStore vectorStore,
            LocalPolicyFileStore localPolicyFileStore) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.policyChunker = policyChunker;
        this.vectorStore = vectorStore;
        this.localPolicyFileStore = localPolicyFileStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PolicyDocument ingest(UUID policyId) {
        PolicyDocument policy = policyDocumentRepository.findById(policyId)
                .orElseThrow(() -> new IllegalStateException("Policy not found: " + policyId));

        try {
            deleteChunks(policyId);
            String text = policy.getContent();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Policy has no content stored in the database");
            }
            List<PolicyChunker.Chunk> chunks = policyChunker.chunk(text);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("Policy content produced no chunks");
            }

            List<Document> documents = chunks.stream()
                    .map(chunk -> toDocument(policy, chunk))
                    .toList();
            vectorStore.add(documents);

            policy.setStatus(PolicyStatus.INDEXED);
            policy.setLastError(null);
            PolicyDocument saved = policyDocumentRepository.save(policy);
            localPolicyFileStore.write(saved);
            log.info("Ingested policy id={} name={} chunks={}", policyId, policy.getName(), documents.size());
            return saved;
        } catch (Exception ex) {
            policy.setStatus(PolicyStatus.FAILED);
            policy.setLastError(truncate(ex.getMessage(), 1000));
            PolicyDocument failed = policyDocumentRepository.save(policy);
            localPolicyFileStore.syncMeta(failed);
            log.error("Failed to ingest policy id={}: {}", policyId, ex.getMessage());
            throw new IllegalStateException("Ingestion failed: " + ex.getMessage(), ex);
        }
    }

    public void deleteChunks(UUID policyId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq(PolicyMetadataKeys.POLICY_ID, policyId.toString()).build());
    }

    private Document toDocument(PolicyDocument policy, PolicyChunker.Chunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(PolicyMetadataKeys.POLICY_ID, policy.getId().toString());
        metadata.put(PolicyMetadataKeys.POLICY_NAME, policy.getName());
        metadata.put(PolicyMetadataKeys.SECTION, chunk.section());
        metadata.put(PolicyMetadataKeys.SOURCE_FILE, policy.getSourceFileName());
        if (policy.getEffectiveDate() != null) {
            metadata.put(PolicyMetadataKeys.EFFECTIVE_DATE, policy.getEffectiveDate().toString());
        }
        if (policy.getGrade() != null && !policy.getGrade().isBlank()) {
            metadata.put(PolicyMetadataKeys.GRADE, policy.getGrade());
        }
        if (policy.getLocation() != null && !policy.getLocation().isBlank()) {
            metadata.put(PolicyMetadataKeys.LOCATION, policy.getLocation());
        }
        return new Document(chunk.text(), metadata);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
