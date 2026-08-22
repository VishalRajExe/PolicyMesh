package com.policymesh.compiler;

import com.policymesh.common.exception.InvalidPolicyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyCompilerTest {

    private final PolicyParser parser = new PolicyParser();
    private final PolicyValidator validator = new PolicyValidator();
    private final PolicyCompiler compiler = new PolicyCompiler(parser, validator);

    private static final String VALID_YAML = """
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

    @Test
    void compilesValidPolicyYaml() {
        CompiledPolicy compiled = compiler.compileFromYaml(VALID_YAML);

        assertThat(compiled.id()).isEqualTo("EU-PII-001");
        assertThat(compiled.jurisdiction()).isEqualTo("EU");
        assertThat(compiled.dataClass()).isEqualTo("PII");
        assertThat(compiled.allowedRegions()).containsExactly("EU");
        assertThat(compiled.deniedRegions()).containsExactlyInAnyOrder("US", "CN");
    }

    @Test
    void rejectsMissingTopLevelKey() {
        String badYaml = "name: not a policy document";

        assertThatThrownBy(() -> compiler.compileFromYaml(badYaml))
                .isInstanceOf(InvalidPolicyException.class)
                .hasMessageContaining("top-level 'policy' key");
    }

    @Test
    void rejectsMissingRequiredFields() {
        String badYaml = """
                policy:
                  name: Missing ID and jurisdiction
                """;

        assertThatThrownBy(() -> compiler.compileFromYaml(badYaml))
                .isInstanceOf(InvalidPolicyException.class)
                .hasMessageContaining("policy.id is required");
    }

    @Test
    void rejectsOverlappingAllowedAndDeniedRegions() {
        String badYaml = """
                policy:
                  id: BAD-001
                  name: Overlapping regions
                  jurisdiction: EU
                  dataClass: PII
                  allowedRegions: [EU, US]
                  deniedRegions: [US]
                """;

        assertThatThrownBy(() -> compiler.compileFromYaml(badYaml))
                .isInstanceOf(InvalidPolicyException.class)
                .hasMessageContaining("cannot both allow and deny");
    }

    @Test
    void rejectsPolicyWithNoRegionsAtAll() {
        String badYaml = """
                policy:
                  id: EMPTY-001
                  name: No regions
                  jurisdiction: EU
                  dataClass: PII
                """;

        assertThatThrownBy(() -> compiler.compileFromYaml(badYaml))
                .isInstanceOf(InvalidPolicyException.class)
                .hasMessageContaining("at least one of allowedRegions or deniedRegions");
    }

    @Test
    void rejectsMalformedYaml() {
        String badYaml = "policy: [this, is, not, a, mapping";

        assertThatThrownBy(() -> compiler.compileFromYaml(badYaml))
                .isInstanceOf(InvalidPolicyException.class);
    }
}
