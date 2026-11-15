package com.acme.hrassistant.application.policies.commands;

import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.domain.PolicyStatus;
import com.acme.hrassistant.rag.LocalPolicyFileStore;
import com.acme.hrassistant.rag.PolicyIngestionService;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadPolicyCommand {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyIngestionService policyIngestionService;
    private final LocalPolicyFileStore localPolicyFileStore;
    private final AppProperties appProperties;

    public UploadPolicyCommand(
            PolicyDocumentRepository policyDocumentRepository,
            PolicyIngestionService policyIngestionService,
            LocalPolicyFileStore localPolicyFileStore,
            AppProperties appProperties) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.policyIngestionService = policyIngestionService;
        this.localPolicyFileStore = localPolicyFileStore;
        this.appProperties = appProperties;
    }

    public PolicyDocumentDto execute(
            EmployeePrincipal admin,
            String name,
            MultipartFile file,
            LocalDate effectiveDate,
            String grade,
            String location) {
        validate(name, file);

        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read upload: " + ex.getMessage(), ex);
        }
        if (text.isBlank()) {
            throw new IllegalStateException("Policy file is empty");
        }

        PolicyDocument saved = policyDocumentRepository.save(new PolicyDocument(
                name.trim(),
                file.getOriginalFilename() == null ? "policy.md" : file.getOriginalFilename(),
                resolveContentType(file),
                text,
                PolicyStatus.PENDING,
                effectiveDate,
                blankToNull(grade),
                blankToNull(location),
                admin.getEmployeeId(),
                Instant.now()));

        localPolicyFileStore.write(saved);
        PolicyDocument indexed = policyIngestionService.ingest(saved.getId());
        localPolicyFileStore.syncMeta(indexed);
        return PolicyDocumentDto.from(indexed);
    }

    private void validate(String name, MultipartFile file) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Policy name is required");
        }
        if (name.length() > 200) {
            throw new IllegalStateException("Policy name must be at most 200 characters");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("Policy file is required");
        }
        long maxBytes = appProperties.getUpload().getMaxBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalStateException("File exceeds max size of " + maxBytes + " bytes");
        }
        String contentType = resolveContentType(file);
        Set<String> allowed = Arrays.stream(appProperties.getUpload().getAllowedTypes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowed.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Unsupported content type: " + contentType + ". Allowed: " + allowed);
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(original.endsWith(".md") || original.endsWith(".markdown") || original.endsWith(".txt"))) {
            throw new IllegalStateException("Only .md / .txt uploads are supported in MVP");
        }
    }

    private static String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() || "application/octet-stream".equals(contentType)) {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            if (name.endsWith(".md") || name.endsWith(".markdown")) {
                return "text/markdown";
            }
            return "text/plain";
        }
        return contentType;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
