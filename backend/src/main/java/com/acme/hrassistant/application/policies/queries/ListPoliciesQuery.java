package com.acme.hrassistant.application.policies.queries;

import com.acme.hrassistant.config.cache.CacheConfig;
import com.acme.hrassistant.domain.PolicyDocumentRepository;
import com.acme.hrassistant.web.dto.PolicyDocumentDto;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPoliciesQuery {

    private final PolicyDocumentRepository policyDocumentRepository;

    public ListPoliciesQuery(PolicyDocumentRepository policyDocumentRepository) {
        this.policyDocumentRepository = policyDocumentRepository;
    }

    @Cacheable(cacheNames = CacheConfig.POLICIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<PolicyDocumentDto> execute() {
        return policyDocumentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PolicyDocumentDto::from)
                .toList();
    }
}
