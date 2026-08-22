package com.policymesh.policy.service.impl;

import com.policymesh.common.response.ApiResponse;
import com.policymesh.policy.dto.PolicyRequest;
import com.policymesh.policy.dto.PolicyResponse;
import com.policymesh.policy.entity.Policy;
import com.policymesh.policy.repository.PolicyRepository;
import com.policymesh.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final ModelMapper modelMapper;

    @Override
    public PolicyResponse createPolicy(PolicyRequest request) {
        // Check if policy with same code already exists
        if (policyRepository.findByPolicyCode(request.getPolicyCode()).isPresent()) {
            throw new RuntimeException("Policy with code " + request.getPolicyCode() + " already exists");
        }

        Policy policy = modelMapper.map(request, Policy.class);
        Policy savedPolicy = policyRepository.save(policy);
        return modelMapper.map(savedPolicy, PolicyResponse.class);
    }

    @Override
    public PolicyResponse getPolicyById(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
        return modelMapper.map(policy, PolicyResponse.class);
    }

    @Override
    public PolicyResponse getPolicyByPolicyCode(String policyCode) {
        Policy policy = policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new RuntimeException("Policy not found with code: " + policyCode));
        return modelMapper.map(policy, PolicyResponse.class);
    }

    @Override
    public List<PolicyResponse> getAllPolicies() {
        List<Policy> policies = policyRepository.findAll();
        return policies.stream()
                .map(policy -> modelMapper.map(policy, PolicyResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public PolicyResponse updatePolicy(Long id, PolicyRequest request) {
        Policy existingPolicy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));

        // Check if another policy with same code exists
        policyRepository.findByPolicyCode(request.getPolicyCode())
                .filter(policy -> !policy.getId().equals(id))
                .ifPresent(policy -> {
                    throw new RuntimeException("Policy with code " + request.getPolicyCode() + " already exists");
                });

        modelMapper.map(request, existingPolicy);
        Policy updatedPolicy = policyRepository.save(existingPolicy);
        return modelMapper.map(updatedPolicy, PolicyResponse.class);
    }

    @Override
    public void deletePolicy(Long id) {
        if (!policyRepository.existsById(id)) {
            throw new RuntimeException("Policy not found with id: " + id);
        }
        policyRepository.deleteById(id);
    }
}