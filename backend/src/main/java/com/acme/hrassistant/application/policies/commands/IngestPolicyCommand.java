package com.acme.hrassistant.application.policies.commands;

import com.acme.hrassistant.config.cache.CacheConfig;
import com.acme.hrassistant.rag.PolicyIngestionService;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestPolicyCommand {

    private final PolicyIngestionService policyIngestionService;

    public IngestPolicyCommand(PolicyIngestionService policyIngestionService) {
        this.policyIngestionService = policyIngestionService;
    }

    @CacheEvict(cacheNames = CacheConfig.POLICIES, allEntries = true)
    @Transactional
    public PolicyDocumentDto execute(UUID id) {
        return PolicyDocumentDto.from(policyIngestionService.ingest(id));
    }
}
