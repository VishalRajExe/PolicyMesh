package com.policymesh;

import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.policy.Decision;
import com.policymesh.policy.PolicyRuleEvaluator;
import com.policymesh.policy.PolicyStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Deterministic policy semantics required by docs/POLICY_DSL.md and the acceptance scenario. */
class PolicyRuleEvaluatorTest {
  private final CompiledPolicy euPii = new CompiledPolicy("EU-PII-001", "EU PII Protection", "EU", "PII",
      Set.of("EU"), Set.of("US", "CN"), PolicyStatus.ACTIVE);
  private final CompiledPolicy inPii = new CompiledPolicy("IN-PII-001", "India PII Protection", "IN", "PII",
      Set.of("IN"), Set.of("US", "CN"), PolicyStatus.ACTIVE);
  private final CompiledPolicy globalPii = new CompiledPolicy("GLOBAL-SENSITIVE-001", "Global Sensitive", "GLOBAL", "PII",
      Set.of("EU", "US", "IN", "CN"), Set.of(), PolicyStatus.ACTIVE);

  @Test
  void canonicalAcceptanceCases() {
    assertThat(eval(List.of(euPii), "EU", "EU", "PII").decision()).isEqualTo(Decision.ALLOW);
    assertThat(eval(List.of(euPii), "EU", "US", "PII").decision()).isEqualTo(Decision.DENY);
    assertThat(eval(List.of(euPii), "EU", "CN", "PII").decision()).isEqualTo(Decision.DENY);
    assertThat(eval(List.of(euPii), "EU", "US", "PUBLIC").decision()).isEqualTo(Decision.ALLOW);
  }

  @Test
  void missingPolicyDeniesByDefaultExceptPublicData() {
    var denied = eval(List.of(), "EU", "US", "PII");
    assertThat(denied.decision()).isEqualTo(Decision.DENY);
    assertThat(denied.policyId()).isNull();
    assertThat(denied.reason()).contains("denied by default");

    assertThat(eval(List.of(), "EU", "US", "NON_SENSITIVE").decision()).isEqualTo(Decision.DENY);
    assertThat(eval(List.of(), "EU", "US", "PUBLIC").decision()).isEqualTo(Decision.ALLOW);
  }

  @Test
  void jurisdictionScopesApplicablePolicies() {
    // With both the EU and India PII policies active, EU->EU stays ALLOW:
    // the India policy is not applicable to an EU-origin transfer.
    assertThat(eval(List.of(euPii, inPii), "EU", "EU", "PII").decision()).isEqualTo(Decision.ALLOW);
    assertThat(eval(List.of(euPii, inPii), "IN", "IN", "PII").decision()).isEqualTo(Decision.ALLOW);
    assertThat(eval(List.of(euPii, inPii), "IN", "EU", "PII").decision()).isEqualTo(Decision.DENY);
    // INDIA is an accepted alias for IN.
    assertThat(eval(List.of(inPii), "INDIA", "IN", "PII").decision()).isEqualTo(Decision.ALLOW);
  }

  @Test
  void denyWinsAcrossAllApplicablePolicies() {
    // GLOBAL allows US but EU-PII-001 denies it: the explicit denial must win.
    var result = eval(List.of(euPii, globalPii), "EU", "US", "PII");
    assertThat(result.decision()).isEqualTo(Decision.DENY);
    assertThat(result.policyId()).isEqualTo("EU-PII-001");
    // Both allow EU -> ALLOW.
    assertThat(eval(List.of(euPii, globalPii), "EU", "EU", "PII").decision()).isEqualTo(Decision.ALLOW);
  }

  @Test
  void destinationNotExplicitlyAllowedIsDenied() {
    var result = eval(List.of(euPii), "EU", "APAC", "PII");
    assertThat(result.decision()).isEqualTo(Decision.DENY);
    assertThat(result.reason()).contains("not explicitly allowed");
  }

  @Test
  void inactivePoliciesNeverApply() {
    CompiledPolicy draft = new CompiledPolicy("EU-PII-001", "EU PII Protection", "EU", "PII",
        Set.of("EU"), Set.of("US", "CN"), PolicyStatus.DRAFT);
    assertThat(eval(List.of(draft), "EU", "US", "PII").decision()).isEqualTo(Decision.DENY);
    assertThat(PolicyRuleEvaluator.applicable(List.of(draft), "EU")).isEmpty();
  }

  @Test
  void worstOfPrefersDeny() {
    var allow = com.policymesh.policy.PolicyEvaluationTestSupport.allow();
    var deny = com.policymesh.policy.PolicyEvaluationTestSupport.deny();
    assertThat(PolicyRuleEvaluator.worstOf(List.of(allow, deny))).isEqualTo(deny);
    assertThat(PolicyRuleEvaluator.worstOf(List.of(allow))).isEqualTo(allow);
  }

  private com.policymesh.policy.PolicyEvaluation eval(List<CompiledPolicy> policies, String source, String destination, String dataClass) {
    return PolicyRuleEvaluator.evaluate(PolicyRuleEvaluator.applicable(policies, source), source, destination, dataClass);
  }
}
