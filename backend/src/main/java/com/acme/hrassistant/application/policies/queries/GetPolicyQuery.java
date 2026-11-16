package com.acme.hrassistant.application.policies.queries;

import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPolicyQuery {

    private final PolicyDocumentRepository policyDocumentRepository;

    public GetPolicyQuery(PolicyDocumentRepository policyDocumentRepository) {
        this.policyDocumentRepository = policyDocumentRepository;
    }

    @Transactional(readOnly = true)
    public PolicyDocumentDto execute(UUID id) {
        return policyDocumentRepository.findById(id)
                .map(PolicyDocumentDto::fromWithContent)
                .orElseThrow(() -> new IllegalStateException("Policy not found: " + id));
    }
}
