package com.policymesh.compiler;

import com.policymesh.policy.entity.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Turns policy YAML (or a persisted {@link Policy} entity) into a
 * {@link CompiledPolicy}. This is the single entry point the rest of the
 * system should use — nobody else should parse YAML directly.
 */
@Component
@RequiredArgsConstructor
public class PolicyCompiler {

    private final PolicyParser parser;
    private final PolicyValidator validator;

    public CompiledPolicy compileFromYaml(String yamlContent) {
        ParsedPolicyDocument doc = parser.parse(yamlContent);
        validator.validate(doc);
        return new CompiledPolicy(
                doc.getId(),
                doc.getName(),
                doc.getJurisdiction().toUpperCase(),
                doc.getDataClass().toUpperCase(),
                new LinkedHashSet<>(doc.getAllowedRegions()),
                new LinkedHashSet<>(doc.getDeniedRegions())
        );
    }

    public CompiledPolicy compileFromEntity(Policy policy) {
        Set<String> allowed = new LinkedHashSet<>(policy.allowedRegionsList());
        Set<String> denied = new LinkedHashSet<>(policy.deniedRegionsList());
        return new CompiledPolicy(
                policy.getPolicyCode(),
                policy.getName(),
                policy.getJurisdiction() == null ? null : policy.getJurisdiction().toUpperCase(),
                policy.getDataClass() == null ? null : policy.getDataClass().toUpperCase(),
                allowed,
                denied
        );
    }
}
