package com.acme.hrassistant.application.policies.commands;

import com.acme.hrassistant.config.cache.CacheConfig;
import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.domain.PolicyStatus;
import com.acme.hrassistant.rag.LocalPolicyFileStore;
import com.acme.hrassistant.rag.PolicyIngestionService;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft-disable: remove vector chunks so chat no longer retrieves this policy; keep DB + local file.
 */
@Service
public class DisablePolicyCommand {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyIngestionService policyIngestionService;
    private final LocalPolicyFileStore localPolicyFileStore;

    public DisablePolicyCommand(
            PolicyDocumentRepository policyDocumentRepository,
            PolicyIngestionService policyIngestionService,
            LocalPolicyFileStore localPolicyFileStore) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.policyIngestionService = policyIngestionService;
        this.localPolicyFileStore = localPolicyFileStore;
    }

    @CacheEvict(cacheNames = CacheConfig.POLICIES, allEntries = true)
    @Transactional
    public PolicyDocumentDto execute(UUID id) {
        PolicyDocument policy = policyDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Policy not found: " + id));

        policyIngestionService.deleteChunks(id);
        policy.setStatus(PolicyStatus.DISABLED);
        policy.setLastError(null);
        PolicyDocument saved = policyDocumentRepository.save(policy);
        localPolicyFileStore.syncMeta(saved);
        return PolicyDocumentDto.from(saved);
    }
}
