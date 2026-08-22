package com.policymesh.compiler;

import com.policymesh.common.ApiException;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.policy.PolicyVocabulary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Raw YAML -> parser -> validation -> CompiledPolicy.
 * Malformed YAML is a 400; a parsable policy that violates the DSL is a 422.
 */
@Service
public class PolicyCompiler {

  public CompiledPolicy compile(String content) {
    if (content == null || content.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "Policy YAML content is required");
    }
    if (content.length() > 1_048_576) {
      throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "oversized-policy", "Policy YAML exceeds maximum allowed size of 1MB");
    }
    Map<?, ?> policy = parse(content);
    CompiledPolicy compiled = new CompiledPolicy(
        required(policy, "id"),
        required(policy, "name"),
        PolicyVocabulary.canonicalRegion(required(policy, "jurisdiction")),
        PolicyVocabulary.canonicalDataClass(required(policy, "dataClass")),
        regionSet(policy.get("allowedRegions"), "allowedRegions"),
        regionSet(policy.get("deniedRegions"), "deniedRegions"),
        status(policy));
    validate(compiled);
    return compiled;
  }

  private Map<?, ?> parse(String content) {
    Object parsed;
    try {
      LoaderOptions loaderOptions = new LoaderOptions();
      loaderOptions.setCodePointLimit(1_048_576);
      parsed = new Yaml(new SafeConstructor(loaderOptions)).load(content);
    } catch (Exception e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "Malformed policy YAML: " + e.getMessage());
    }
    if (!(parsed instanceof Map<?, ?> root) || !(root.get("policy") instanceof Map<?, ?> policy)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "YAML must contain a top-level 'policy' object");
    }
    return policy;
  }

  private String required(Map<?, ?> policy, String key) {
    Object value = policy.get(key);
    if (value == null || value.toString().isBlank()) {
      throw ApiException.unprocessable("Policy field '" + key + "' is required");
    }
    return value.toString().trim();
  }

  private Set<String> regionSet(Object value, String field) {
    if (value == null) return new TreeSet<>();
    if (!(value instanceof Collection<?> collection)) {
      throw ApiException.unprocessable("Policy field '" + field + "' must be a list of regions");
    }
    TreeSet<String> regions = new TreeSet<>();
    for (Object item : collection) {
      if (item == null || item.toString().isBlank()) {
        throw ApiException.unprocessable("Policy field '" + field + "' contains a blank region");
      }
      regions.add(PolicyVocabulary.canonicalRegion(item.toString()));
    }
    return regions;
  }

  private PolicyStatus status(Map<?, ?> policy) {
    Object value = policy.get("status");
    if (value == null) return PolicyStatus.ACTIVE;
    try {
      return PolicyStatus.valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw ApiException.unprocessable("Policy status must be one of DRAFT, ACTIVE, INACTIVE");
    }
  }

  private void validate(CompiledPolicy p) {
    if (!p.policyCode().matches("[A-Z0-9][A-Z0-9_-]*")) {
      throw ApiException.unprocessable("Policy id must match ^[A-Z0-9][A-Z0-9_-]*$ (got '" + p.policyCode() + "')");
    }
    if (!PolicyVocabulary.isKnownDataClass(p.dataClass())) {
      throw ApiException.unprocessable("Unknown dataClass '" + p.dataClass() + "'; known classes: " + PolicyVocabulary.DATA_CLASSES);
    }
    if (p.allowedRegions().isEmpty()) {
      throw ApiException.unprocessable("allowedRegions must contain at least one region");
    }
    for (String region : p.allowedRegions()) {
      if (p.deniedRegions().contains(region)) {
        throw ApiException.unprocessable("allowedRegions and deniedRegions must not overlap (region " + region + ")");
      }
    }
  }
}
