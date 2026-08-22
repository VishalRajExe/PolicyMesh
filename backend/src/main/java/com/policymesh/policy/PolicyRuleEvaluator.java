package com.policymesh.policy;

import com.policymesh.compiler.CompiledPolicy;

import java.util.List;
import java.util.Set;

/**
 * The single authoritative policy decision logic. Every consumer (runtime enforcement,
 * graph analyzer, CI checker, CLI) funnels through this class — never through a copy.
 *
 * Resolution order, per docs/GRAPH_ENGINE.md and policies/README.md:
 * 1. Applicable policies = ACTIVE, same dataClass, jurisdiction equal to the source region
 *    (GLOBAL jurisdiction applies everywhere).
 * 2. No applicable policy -> deny by default, except PUBLIC data which needs no policy.
 * 3. Among applicable policies deny wins: an explicit deniedRegions hit denies outright;
 *    a destination absent from any policy's allowedRegions is also denied ("not explicitly allowed").
 * 4. Otherwise ALLOW.
 * REROUTE is a defined outcome reserved for future use; no policy field triggers it yet.
 */
public final class PolicyRuleEvaluator {
  private PolicyRuleEvaluator() {}

  public static List<CompiledPolicy> applicable(List<CompiledPolicy> policies, String sourceRegion) {
    String source = PolicyVocabulary.canonicalRegion(sourceRegion);
    return policies.stream()
        .filter(p -> p.status() == PolicyStatus.ACTIVE)
        .filter(p -> PolicyVocabulary.GLOBAL_JURISDICTION.equals(p.jurisdiction()) || p.jurisdiction().equals(source))
        .sorted(java.util.Comparator.comparing(CompiledPolicy::policyCode))
        .toList();
  }

  public static PolicyEvaluation evaluate(List<CompiledPolicy> applicablePolicies,
                                          String sourceRegion, String destinationRegion, String dataClass) {
    String destination = PolicyVocabulary.canonicalRegion(destinationRegion);
    String data = PolicyVocabulary.canonicalDataClass(dataClass);
    List<CompiledPolicy> applicable = applicablePolicies.stream()
        .filter(p -> p.dataClass().equals(data))
        .toList();

    if (applicable.isEmpty()) {
      if (PolicyVocabulary.PUBLIC_DATA_CLASS.equals(data)) {
        return new PolicyEvaluation(Decision.ALLOW, null,
            "Public data requires no policy; transfer to " + destination + " is allowed");
      }
      return new PolicyEvaluation(Decision.DENY, null,
          "No active policy applies to data class " + data + " from " + PolicyVocabulary.canonicalRegion(sourceRegion)
              + "; denied by default");
    }

    for (CompiledPolicy policy : applicable) {
      if (policy.deniedRegions().contains(destination)) {
        return new PolicyEvaluation(Decision.DENY, policy.policyCode(),
            "Destination region " + destination + " is denied by policy " + policy.policyCode());
      }
    }
    for (CompiledPolicy policy : applicable) {
      if (!policy.allowedRegions().contains(destination)) {
        return new PolicyEvaluation(Decision.DENY, policy.policyCode(),
            "Destination region " + destination + " is not explicitly allowed by policy " + policy.policyCode());
      }
    }
    return new PolicyEvaluation(Decision.ALLOW, applicable.getFirst().policyCode(),
        "All applicable policies (" + applicable.size() + ") allow destination region " + destination);
  }

  /** Worst-case combination used when a transfer carries multiple data classes. */
  public static PolicyEvaluation worstOf(Iterable<PolicyEvaluation> evaluations) {
    PolicyEvaluation worst = null;
    for (PolicyEvaluation evaluation : evaluations) {
      if (worst == null || severity(evaluation) > severity(worst)) worst = evaluation;
    }
    return worst;
  }

  private static int severity(PolicyEvaluation evaluation) {
    return switch (evaluation.decision()) {
      case DENY -> 2;
      case REROUTE -> 1;
      case ALLOW -> 0;
    };
  }

  public static Set<String> canonicalDataClasses(Iterable<String> dataClasses) {
    Set<String> result = new java.util.TreeSet<>();
    for (String dataClass : dataClasses) {
      String canonical = PolicyVocabulary.canonicalDataClass(dataClass);
      if (!canonical.isBlank()) result.add(canonical);
    }
    return result;
  }
}
