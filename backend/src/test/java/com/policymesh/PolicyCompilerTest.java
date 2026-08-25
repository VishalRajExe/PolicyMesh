package com.policymesh;

import com.policymesh.common.ApiException;
import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.compiler.PolicyCompiler;
import com.policymesh.policy.PolicyStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyCompilerTest {
  private final PolicyCompiler compiler = new PolicyCompiler();

  private static final String VALID_MINIMAL = """
      policy:
        id: EU-PII-001
        name: EU PII Protection
        jurisdiction: EU
        dataClass: PII
        allowedRegions:
          - EU
        deniedRegions:
          - US
          - CN
      """;

  private static final String VALID_FULL = """
      policy:
        id: IN-PII-001
        name: India PII Protection
        version: 1
        status: ACTIVE
        jurisdiction: INDIA
        dataClass: PII
        description: demo policy
        allowedRegions: [IN]
        deniedRegions: [US, CN]
        enforcement:
          ci: true
          runtime: true
        defaultDecision: DENY
      """;

  @Test
  void compilesMinimalDsl() {
    CompiledPolicy compiled = compiler.compile(VALID_MINIMAL);
    assertThat(compiled.policyCode()).isEqualTo("EU-PII-001");
    assertThat(compiled.jurisdiction()).isEqualTo("EU");
    assertThat(compiled.dataClass()).isEqualTo("PII");
    assertThat(compiled.allowedRegions()).containsExactly("EU");
    assertThat(compiled.deniedRegions()).containsExactlyInAnyOrder("US", "CN");
    assertThat(compiled.status()).isEqualTo(PolicyStatus.ACTIVE);
  }

  @Test
  void compilesFullDslAndNormalizesJurisdictionAlias() {
    CompiledPolicy compiled = compiler.compile(VALID_FULL);
    assertThat(compiled.policyCode()).isEqualTo("IN-PII-001");
    assertThat(compiled.jurisdiction()).isEqualTo("IN"); // INDIA alias folded onto IN
    assertThat(compiled.status()).isEqualTo(PolicyStatus.ACTIVE);
  }

  @Test
  void malformedYamlIsRejectedAsBadRequest() {
    assertThatThrownBy(() -> compiler.compile("policy: [broken"))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(400));
    assertThatThrownBy(() -> compiler.compile("just: a string"))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(400));
  }

  @Test
  void missingRequiredFieldsAreUnprocessable() {
    assertThatThrownBy(() -> compiler.compile("policy:\n  id: EU-PII-001\n"))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(422));
  }

  @Test
  void overlappingRegionsAreUnprocessable() {
    assertThatThrownBy(() -> compiler.compile("""
        policy:
          id: EU-PII-001
          name: n
          jurisdiction: EU
          dataClass: PII
          allowedRegions: [EU]
          deniedRegions: [EU]
        """))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(422))
        .hasMessageContaining("overlap");
  }

  @Test
  void emptyAllowedRegionsAreUnprocessable() {
    assertThatThrownBy(() -> compiler.compile("""
        policy:
          id: EU-PII-001
          name: n
          jurisdiction: EU
          dataClass: PII
          allowedRegions: []
        """))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(422));
  }

  @Test
  void unknownDataClassAndBadIdAreUnprocessable() {
    assertThatThrownBy(() -> compiler.compile("""
        policy:
          id: EU-WEIRD-001
          name: n
          jurisdiction: EU
          dataClass: SUPER_SECRET
          allowedRegions: [EU]
        """))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(422))
        .hasMessageContaining("dataClass");
    assertThatThrownBy(() -> compiler.compile("""
        policy:
          id: bad id!
          name: n
          jurisdiction: EU
          dataClass: PII
          allowedRegions: [EU]
        """))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).status().value()).isEqualTo(422));
  }

  @Test
  void compilesFlatYamlWithoutPolicyWrapper() {
    String flatYaml = """
        policyCode: EU-PII-002
        name: Strict EU Cross-Border Residency
        jurisdiction: EU
        dataClass: PII
        allowedRegions:
          - EU
        deniedRegions:
          - US
        status: ACTIVE
        """;
    CompiledPolicy compiled = compiler.compile(flatYaml);
    assertThat(compiled.policyCode()).isEqualTo("EU-PII-002");
    assertThat(compiled.name()).isEqualTo("Strict EU Cross-Border Residency");
    assertThat(compiled.jurisdiction()).isEqualTo("EU");
    assertThat(compiled.dataClass()).isEqualTo("PII");
    assertThat(compiled.allowedRegions()).containsExactly("EU");
    assertThat(compiled.deniedRegions()).containsExactly("US");
    assertThat(compiled.status()).isEqualTo(PolicyStatus.ACTIVE);
  }

  @Test
  void compilesMultiPolicyBundle() {
    String bundleYaml = """
        policies:
          - policyCode: GLOBAL-PII-001
            name: Global Rule
            jurisdiction: GLOBAL
            dataClass: PII
            allowedRegions: EU, US
            deniedRegions: CN
          - policyCode: EU-PCI-001
            name: EU Payment Rule
            jurisdiction: EU
            dataClass: PCI
            allowedRegions: [EU]
            deniedRegions: [US]
        """;
    var list = compiler.compileAll(bundleYaml);
    assertThat(list).hasSize(2);
    assertThat(list.get(0).policyCode()).isEqualTo("GLOBAL-PII-001");
    assertThat(list.get(0).allowedRegions()).containsExactlyInAnyOrder("EU", "US");
    assertThat(list.get(1).policyCode()).isEqualTo("EU-PCI-001");
  }
}
