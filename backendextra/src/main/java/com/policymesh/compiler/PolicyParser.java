package com.policymesh.compiler;

import com.policymesh.common.exception.InvalidPolicyException;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the PolicyMesh YAML policy DSL into a {@link ParsedPolicyDocument}.
 * Nothing outside the compiler package should ever touch a raw YAML string.
 *
 * Expected shape:
 * <pre>
 * policy:
 *   id: EU-PII-001
 *   name: EU PII Protection
 *   jurisdiction: EU
 *   dataClass: PII
 *   allowedRegions: [EU]
 *   deniedRegions: [US, CN]
 * </pre>
 */
@Component
public class PolicyParser {

    private final Yaml yaml = new Yaml();

    @SuppressWarnings("unchecked")
    public ParsedPolicyDocument parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new InvalidPolicyException("Policy document is empty");
        }

        Map<String, Object> root;
        try {
            root = yaml.load(yamlContent);
        } catch (Exception e) {
            throw new InvalidPolicyException("Malformed YAML: " + e.getMessage());
        }

        if (root == null || !root.containsKey("policy")) {
            throw new InvalidPolicyException("Policy document must have a top-level 'policy' key");
        }

        Object policyObj = root.get("policy");
        if (!(policyObj instanceof Map)) {
            throw new InvalidPolicyException("'policy' must be a mapping");
        }
        Map<String, Object> node = (Map<String, Object>) policyObj;

        ParsedPolicyDocument doc = new ParsedPolicyDocument();
        doc.setId(asString(node.get("id")));
        doc.setName(asString(node.get("name")));
        doc.setJurisdiction(asString(node.get("jurisdiction")));
        doc.setDataClass(asString(node.get("dataClass")));
        doc.setAllowedRegions(asStringList(node.get("allowedRegions")));
        doc.setDeniedRegions(asStringList(node.get("deniedRegions")));
        return doc;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(o -> o == null ? "" : o.toString().trim().toUpperCase()).toList();
        }
        if (value instanceof Map) {
            // Defensive: not expected, but avoid ClassCastException on odd input.
            throw new InvalidPolicyException("Region lists must be YAML sequences, not mappings");
        }
        return List.of(asString(value).toUpperCase());
    }
}
