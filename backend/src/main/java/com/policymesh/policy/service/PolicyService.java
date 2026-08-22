package com.policymesh.policy.service;

import com.policymesh.policy.dto.PolicyRequest;
import com.policymesh.policy.dto.PolicyResponse;
import com.policymesh.policy.entity.Policy;

import java.util List;

public interface PolicyService {
    PolicyResponse createPolicy(PolicyRequest request);
    PolicyResponse getPolicyById(Long id);
    PolicyResponse getPolicyByPolicyCode(String policyCode);
    List<PolicyResponse> getAllPolicies();
    PolicyResponse updatePolicy(Long id, PolicyRequest request);
    void deletePolicy(Long id);
}