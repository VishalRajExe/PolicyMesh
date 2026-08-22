package com.policymesh.policy;

/** Small helpers shared by pure unit tests. */
public final class PolicyEvaluationTestSupport {
  private PolicyEvaluationTestSupport() {}

  public static PolicyEvaluation allow() { return new PolicyEvaluation(Decision.ALLOW, "P-ALLOW", "allowed"); }
  public static PolicyEvaluation deny() { return new PolicyEvaluation(Decision.DENY, "P-DENY", "denied"); }
}
