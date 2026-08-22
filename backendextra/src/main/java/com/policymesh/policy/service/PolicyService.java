package com.policymesh.policy.service;

import com.policymesh.common.event.EventPublisherService;
import com.policymesh.common.exception.DuplicateResourceException;
import com.policymesh.common.exception.ResourceNotFoundException;
import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.compiler.PolicyCompiler;
import com.policymesh.config.KafkaConfig;
import com.policymesh.policy.dto.PolicyRequest;
import com.policymesh.policy.dto.PolicyResponse;
import com.policymesh.policy.dto.PolicyYamlRequest;
import com.policymesh.policy.entity.Policy;
import com.policymesh.policy.entity.PolicyStatus;
import com.policymesh.policy.repository.PolicyRepository;
import com.policymesh.policy.validator.PolicyRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyCompiler policyCompiler;
    private final PolicyRequestValidator policyRequestValidator;
    private final PolicyCacheService policyCacheService;
    private final EventPublisherService eventPublisherService;

    @Transactional(readOnly = true)
    public List<PolicyResponse> findAll() {
        return policyRepository.findAll().stream().map(PolicyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponse findById(UUID id) {
        return PolicyResponse.from(getOrThrow(id));
    }

    @Transactional
    public PolicyResponse create(PolicyRequest request) {
        policyRequestValidator.validate(request);
        if (policyRepository.existsByPolicyCode(request.policyCode())) {
            throw new DuplicateResourceException("Policy with code '" + request.policyCode() + "' already exists");
        }
        Policy policy = Policy.builder()
                .policyCode(request.policyCode())
                .name(request.name())
                .jurisdiction(request.jurisdiction().toUpperCase())
                .dataClass(request.dataClass().toUpperCase())
                .allowedRegions(joinRegions(request.allowedRegions()))
                .deniedRegions(joinRegions(request.deniedRegions()))
                .status(PolicyStatus.ACTIVE)
                .version(1)
                .build();

        // Compile once up-front to fail fast on invalid policies before persisting.
        policyCompiler.compileFromEntity(policy);

        Policy saved = policyRepository.save(policy);
        policyCacheService.evictAll();
        eventPublisherService.publish(KafkaConfig.TOPIC_POLICY_UPDATED, saved.getPolicyCode(),
                Map.of("action", "created", "policyCode", saved.getPolicyCode()));
        return PolicyResponse.from(saved);
    }

    @Transactional
    public PolicyResponse createFromYaml(PolicyYamlRequest request) {
        CompiledPolicy compiled = policyCompiler.compileFromYaml(request.yaml());
        if (policyRepository.existsByPolicyCode(compiled.id())) {
            throw new DuplicateResourceException("Policy with code '" + compiled.id() + "' already exists");
        }
        Policy policy = Policy.builder()
                .policyCode(compiled.id())
                .name(compiled.name())
                .jurisdiction(compiled.jurisdiction())
                .dataClass(compiled.dataClass())
                .allowedRegions(String.join(",", compiled.allowedRegions()))
                .deniedRegions(String.join(",", compiled.deniedRegions()))
                .status(PolicyStatus.ACTIVE)
                .version(1)
                .build();
        Policy saved = policyRepository.save(policy);
        policyCacheService.evictAll();
        eventPublisherService.publish(KafkaConfig.TOPIC_POLICY_UPDATED, saved.getPolicyCode(),
                Map.of("action", "created-from-yaml", "policyCode", saved.getPolicyCode()));
        return PolicyResponse.from(saved);
    }

    @Transactional
    public PolicyResponse update(UUID id, PolicyRequest request) {
        policyRequestValidator.validate(request);
        Policy policy = getOrThrow(id);

        if (!policy.getPolicyCode().equals(request.policyCode())
                && policyRepository.existsByPolicyCode(request.policyCode())) {
            throw new DuplicateResourceException("Policy with code '" + request.policyCode() + "' already exists");
        }

        policy.setPolicyCode(request.policyCode());
        policy.setName(request.name());
        policy.setJurisdiction(request.jurisdiction().toUpperCase());
        policy.setDataClass(request.dataClass().toUpperCase());
        policy.setAllowedRegions(joinRegions(request.allowedRegions()));
        policy.setDeniedRegions(joinRegions(request.deniedRegions()));
        policy.setVersion(policy.getVersion() + 1);

        // Ensure the updated policy still compiles cleanly.
        policyCompiler.compileFromEntity(policy);

        Policy saved = policyRepository.save(policy);
        policyCacheService.evictAll();
        eventPublisherService.publish(KafkaConfig.TOPIC_POLICY_UPDATED, saved.getPolicyCode(),
                Map.of("action", "updated", "policyCode", saved.getPolicyCode()));
        return PolicyResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Policy policy = getOrThrow(id);
        policyRepository.delete(policy);
        policyCacheService.evictAll();
        eventPublisherService.publish(KafkaConfig.TOPIC_POLICY_UPDATED, policy.getPolicyCode(),
                Map.of("action", "deleted", "policyCode", policy.getPolicyCode()));
    }

    /**
     * Resolves the compiled, cache-aware policy applicable to a given
     * jurisdiction (region) + data class pair. Used by the policy engine.
     * Cache-first, DB-fallback, per the Redis caching requirement.
     */
    @Transactional(readOnly = true)
    public Optional<CompiledPolicy> resolveCompiledPolicy(String jurisdiction, String dataClass) {
        Optional<CompiledPolicy> cached = policyCacheService.get(jurisdiction, dataClass);
        if (cached.isPresent()) {
            return cached;
        }

        List<Policy> candidates = policyRepository.findByDataClassIgnoreCaseAndStatus(dataClass, PolicyStatus.ACTIVE);
        Optional<Policy> match = candidates.stream()
                .filter(p -> p.getJurisdiction().equalsIgnoreCase(jurisdiction))
                .findFirst();

        if (match.isEmpty()) {
            return Optional.empty();
        }

        CompiledPolicy compiled = policyCompiler.compileFromEntity(match.get());
        policyCacheService.put(jurisdiction, dataClass, compiled);
        return Optional.of(compiled);
    }

    /** Fetches ALL active compiled policies for a data class, regardless of jurisdiction match, for graph-wide checks. */
    @Transactional(readOnly = true)
    public List<CompiledPolicy> resolveCompiledPoliciesForDataClass(String dataClass) {
        return policyRepository.findByDataClassIgnoreCaseAndStatus(dataClass, PolicyStatus.ACTIVE).stream()
                .map(policyCompiler::compileFromEntity)
                .toList();
    }

    private Policy getOrThrow(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    private String joinRegions(List<String> regions) {
        if (regions == null || regions.isEmpty()) return "";
        return String.join(",", regions.stream().map(String::toUpperCase).toList());
    }
}
