package com.policymesh.compiler;

import com.policymesh.policy.dto.PolicyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util Map;

@Component
@RequiredArgsConstructor
public class PolicyParser {

    private final Yaml yaml;

    public CompiledPolicy parse(String yamlContent) {
        Map<String, Object> data = yaml.load(yamlContent);
        Map<String, Object> policyMap = (Map<String, Object>) data.get("policy");

        return CompiledPolicy.builder()
                .policyCode((String) policyMap.get("id"))
                .name((String) policyMap.get("name"))
                .jurisdiction((String) policyMap.get("jurisdiction"))
                .dataClass((String) policyMap.get("dataClass"))
                .allowedRegions((List<String>) policyMap.get("allowedRegions"))
                .deniedRegions((List<String>) policyMap.get("deniedRegions"))
                .build();
    }

    public String toYaml(CompiledPolicy policy) {
        // This would convert CompiledPolicy back to YAML
        // For simplicity, we're not implementing the full round-trip here
        return "";
    }

    public PolicyRequest toPolicyRequest(CompiledPolicy compiledPolicy) {
        return PolicyRequest.builder()
                .policyCode(compiledPolicy.getPolicyCode())
                .name(compiledPolicy.getName())
                .jurisdiction(compiledPolicy.getJurisdiction())
                .dataClass(compiledPolicy.getDataClass())
                .allowedRegions(compiledPolicy.getAllowedRegions())
                .deniedRegions(compiledPolicy.getDeniedRegions())
                .status("ACTIVE")
                .version(1)
                .build();
    }
}