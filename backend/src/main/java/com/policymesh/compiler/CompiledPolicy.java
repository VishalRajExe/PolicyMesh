package com.policymesh.compiler;

import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.policy.PolicyVocabulary;

import java.util.Set;
import java.util.TreeSet;

/** The internal, validation-complete policy model produced by the compiler and consumed by the engine. */
public record CompiledPolicy(String policyCode,
                             String name,
                             String jurisdiction,
                             String dataClass,
                             Set<String> allowedRegions,
                             Set<String> deniedRegions,
                             PolicyStatus status) {

  public static CompiledPolicy from(Policy policy) {
    return new CompiledPolicy(
        policy.getPolicyCode(),
        policy.getName(),
        PolicyVocabulary.canonicalRegion(policy.getJurisdiction()),
        PolicyVocabulary.canonicalDataClass(policy.getDataClass()),
        new TreeSet<>(policy.getAllowedRegions()),
        new TreeSet<>(policy.getDeniedRegions()),
        policy.getStatus());
  }
}
