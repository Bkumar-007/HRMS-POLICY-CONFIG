package com.acme.hrassistant.rag;

import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.domain.PolicyDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Writes policy text + metadata under a local folder so files remain readable
 * even when Docker/Postgres is stopped (e.g. {@code backend/data/policies/}).
 */
@Service
public class LocalPolicyFileStore {

    private static final Logger log = LoggerFactory.getLogger(LocalPolicyFileStore.class);

    private final Path root;
    private final ObjectMapper objectMapper;

    public LocalPolicyFileStore(AppProperties appProperties) throws IOException {
        this.root = Path.of(appProperties.getUpload().getLocalDir()).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        log.info("Local policy files directory: {}", this.root);
    }

    public Path root() {
        return root;
    }

    public String write(PolicyDocument policy) {
        try {
            Path dir = dirFor(policy.getId());
            Files.createDirectories(dir);
            String safeName = sanitizeFileName(policy.getSourceFileName());
            Path contentFile = dir.resolve(safeName);
            Files.writeString(contentFile, policy.getContent() == null ? "" : policy.getContent(), StandardCharsets.UTF_8);
            writeMeta(policy, contentFile);
            return contentFile.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write local policy file: " + ex.getMessage(), ex);
        }
    }

    public void syncMeta(PolicyDocument policy) {
        try {
            Path dir = dirFor(policy.getId());
            if (!Files.isDirectory(dir)) {
                write(policy);
                return;
            }
            Path contentFile = dir.resolve(sanitizeFileName(policy.getSourceFileName()));
            if (!Files.exists(contentFile)) {
                // pick first non-meta file if name changed
                try (var stream = Files.list(dir)) {
                    contentFile = stream
                            .filter(p -> !p.getFileName().toString().equals("meta.json"))
                            .findFirst()
                            .orElse(contentFile);
                }
            }
            writeMeta(policy, contentFile);
        } catch (IOException ex) {
            log.warn("Could not sync local meta for policy {}: {}", policy.getId(), ex.getMessage());
        }
    }

    public void delete(UUID policyId) {
        Path dir = dirFor(policyId);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Could not delete {}: {}", path, ex.getMessage());
                }
            });
        } catch (IOException ex) {
            log.warn("Could not delete local policy dir {}: {}", dir, ex.getMessage());
        }
    }

    private void writeMeta(PolicyDocument policy, Path contentFile) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", policy.getId() == null ? null : policy.getId().toString());
        meta.put("name", policy.getName());
        meta.put("sourceFileName", policy.getSourceFileName());
        meta.put("contentType", policy.getContentType());
        meta.put("status", policy.getStatus() == null ? null : policy.getStatus().name());
        meta.put("effectiveDate", policy.getEffectiveDate());
        meta.put("grade", policy.getGrade());
        meta.put("location", policy.getLocation());
        meta.put("uploadedBy", policy.getUploadedBy());
        meta.put("createdAt", policy.getCreatedAt());
        meta.put("localFile", contentFile.getFileName().toString());
        meta.put("note", "Readable copy for local browsing when Docker/Postgres is down. App still uses DB + pgvector when running.");
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(dirFor(policy.getId()).resolve("meta.json").toFile(), meta);
    }

    private Path dirFor(UUID policyId) {
        return root.resolve(policyId.toString());
    }

    private static String sanitizeFileName(String original) {
        String name = (original == null || original.isBlank()) ? "policy.md" : original;
        name = name.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
        return name.isBlank() ? "policy.md" : name;
    }
}
