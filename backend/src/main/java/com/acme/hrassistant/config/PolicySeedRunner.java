package com.acme.hrassistant.config;

import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.domain.PolicyStatus;
import com.acme.hrassistant.rag.LocalPolicyFileStore;
import com.acme.hrassistant.rag.PolicyIngestionService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * When the policy library is empty, seed from classpath {@code seed-policies/*.md}
 * into Postgres ({@code policy_documents.content}) and ingest into pgvector.
 */
@Component
@Order(100)
@Profile("!test")
public class PolicySeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicySeedRunner.class);

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyIngestionService policyIngestionService;
    private final LocalPolicyFileStore localPolicyFileStore;
    private final AppProperties appProperties;

    public PolicySeedRunner(
            PolicyDocumentRepository policyDocumentRepository,
            PolicyIngestionService policyIngestionService,
            LocalPolicyFileStore localPolicyFileStore,
            AppProperties appProperties) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.policyIngestionService = policyIngestionService;
        this.localPolicyFileStore = localPolicyFileStore;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!appProperties.getRag().isSeedOnEmpty()) {
            return;
        }
        if (policyDocumentRepository.count() > 0) {
            log.info("Policy library already has {} document(s); skipping seed", policyDocumentRepository.count());
            return;
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:seed-policies/*.md");
        if (resources.length == 0) {
            log.warn("No classpath:seed-policies/*.md found; policy library remains empty");
            return;
        }

        List<Resource> sorted = List.of(resources).stream()
                .sorted(Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)))
                .toList();

        Long systemUploader = 0L;
        for (Resource resource : sorted) {
            String fileName = resource.getFilename() == null ? "policy.md" : resource.getFilename();
            String content;
            try (InputStream in = resource.getInputStream()) {
                content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            SeedMeta meta = parseMeta(fileName, content);
            PolicyDocument saved = policyDocumentRepository.save(new PolicyDocument(
                    meta.name(),
                    fileName,
                    "text/markdown",
                    content,
                    PolicyStatus.PENDING,
                    meta.effectiveDate(),
                    meta.grade(),
                    meta.location(),
                    systemUploader,
                    Instant.now()));
            localPolicyFileStore.write(saved);
            try {
                policyIngestionService.ingest(saved.getId());
                log.info("Seeded policy '{}' from {}", meta.name(), fileName);
            } catch (RuntimeException ex) {
                log.error("Failed to seed {}: {}", fileName, ex.getMessage());
            }
        }
    }

    private static SeedMeta parseMeta(String fileName, String content) {
        String name = humanize(fileName);
        LocalDate effectiveDate = LocalDate.of(2026, 1, 1);
        String grade = null;
        String location = null;

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<!--") && trimmed.contains(":")) {
                String body = trimmed.replace("<!--", "").replace("-->", "").trim();
                String[] parts = body.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();
                    switch (key) {
                        case "name" -> name = value;
                        case "grade" -> grade = blankToNull(value);
                        case "location" -> location = blankToNull(value);
                        case "effective" -> {
                            try {
                                effectiveDate = LocalDate.parse(value);
                            } catch (Exception ignored) {
                                // keep default
                            }
                        }
                        default -> {
                        }
                    }
                }
            } else if (!trimmed.isEmpty() && !trimmed.startsWith("<!--")) {
                break;
            }
        }
        return new SeedMeta(name, effectiveDate, grade, location);
    }

    private static String humanize(String fileName) {
        String base = fileName.replaceAll("(?i)\\.md$", "").replace('-', ' ').replace('_', ' ').trim();
        if (base.isEmpty()) {
            return "Untitled Policy";
        }
        String[] words = base.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() || "ALL".equalsIgnoreCase(value) ? null : value;
    }

    private record SeedMeta(String name, LocalDate effectiveDate, String grade, String location) {
    }
}
