package com.policymesh.compiler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyCompiler {

    private final PolicyParser policyParser;
    private final PolicyValidator policyValidator;

    public CompiledPolicy compile(String yamlContent) {
        CompiledPolicy compiledPolicy = policyParser.parse(yamlContent);
        policyValidator.validate(compiledPolicy);
        return compiledPolicy;
    }

    public String decompile(CompiledPolicy policy) {
        // In a full implementation, this would convert CompiledPolicy back to YAML
        // For now, we'll return a placeholder
        return policyParser.toYaml(policy);
    }
}