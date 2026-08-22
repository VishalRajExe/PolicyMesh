package com.policymesh.policy.repository;

import com.policymesh.policy.entity.Policy;
import com.policymesh.policy.entity.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    Optional<Policy> findByPolicyCode(String policyCode);
    boolean existsByPolicyCode(String policyCode);
    List<Policy> findByStatus(PolicyStatus status);
    List<Policy> findByDataClassIgnoreCaseAndStatus(String dataClass, PolicyStatus status);
    long countByStatus(PolicyStatus status);
}
