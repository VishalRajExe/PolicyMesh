package com.policymesh.policy.repository;

import com.policymesh.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyCode(String policyCode);
    List<Policy> findByDataClass(String dataClass);
}