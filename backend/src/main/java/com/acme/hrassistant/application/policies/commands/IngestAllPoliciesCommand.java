package com.acme.hrassistant.application.policies.commands;

import com.acme.hrassistant.config.cache.CacheConfig;
import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.rag.PolicyIngestionService;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class IngestAllPoliciesCommand {

    private static final Logger log = LoggerFactory.getLogger(IngestAllPoliciesCommand.class);

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyIngestionService policyIngestionService;

    public IngestAllPoliciesCommand(
            PolicyDocumentRepository policyDocumentRepository,
            PolicyIngestionService policyIngestionService) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.policyIngestionService = policyIngestionService;
    }

    @CacheEvict(cacheNames = CacheConfig.POLICIES, allEntries = true)
    public List<PolicyDocumentDto> execute() {
        List<PolicyDocumentDto> results = new ArrayList<>();
        for (PolicyDocument policy : policyDocumentRepository.findAllByOrderByCreatedAtDesc()) {
            try {
                results.add(PolicyDocumentDto.from(policyIngestionService.ingest(policy.getId())));
            } catch (RuntimeException ex) {
                log.warn("ingest-all failed for {}: {}", policy.getId(), ex.getMessage());
                policyDocumentRepository.findById(policy.getId())
                        .map(PolicyDocumentDto::from)
                        .ifPresent(results::add);
            }
        }
        return results;
    }
}
