package com.acme.hrassistant.application.policies.commands;

import com.acme.hrassistant.config.cache.CacheConfig;
import com.acme.hrassistant.domain.PolicyDocument;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.rag.LocalPolicyFileStore;
import com.acme.hrassistant.rag.PolicyIngestionService;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeletePolicyCommand {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyIngestionService policyIngestionService;
    private final LocalPolicyFileStore localPolicyFileStore;

    public DeletePolicyCommand(
            PolicyDocumentRepository policyDocumentRepository,
            PolicyIngestionService policyIngestionService,
            LocalPolicyFileStore localPolicyFileStore) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.policyIngestionService = policyIngestionService;
        this.localPolicyFileStore = localPolicyFileStore;
    }

    @CacheEvict(cacheNames = CacheConfig.POLICIES, allEntries = true)
    @Transactional
    public void execute(UUID id) {
        PolicyDocument policy = policyDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Policy not found: " + id));

        policyIngestionService.deleteChunks(id);
        policyDocumentRepository.delete(policy);
        localPolicyFileStore.delete(id);
    }
}
