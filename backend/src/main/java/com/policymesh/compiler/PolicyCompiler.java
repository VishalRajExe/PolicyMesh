package com.policymesh.compiler;

import com.policymesh.common.ApiException;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.policy.PolicyVocabulary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Raw YAML -> parser -> validation -> CompiledPolicy.
 * Supports:
 * - Traditional wrapped: `policy: { id, ... }`
 * - Flat root: `{ policyCode/id, name, jurisdiction, dataClass, ... }`
 * - Multi-policy list: `policies: [ ... ]` or list of YAML maps or multi-docs `---`
 */
@Service
public class PolicyCompiler {

  public CompiledPolicy compile(String content) {
    List<CompiledPolicy> list = compileAll(content);
    if (list.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "No policy found in YAML specification");
    }
    return list.get(0);
  }

  public List<CompiledPolicy> compileAll(String content) {
    if (content == null || content.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "Policy YAML content is required");
    }
    if (content.length() > 1_048_576) {
      throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "oversized-policy", "Policy YAML exceeds maximum allowed size of 1MB");
    }

    List<Map<?, ?>> policyMaps = parseAll(content);
    if (policyMaps.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "YAML does not contain valid policy definitions");
    }

    List<CompiledPolicy> result = new ArrayList<>();
    for (Map<?, ?> policy : policyMaps) {
      CompiledPolicy compiled = new CompiledPolicy(
          required(policy, "id", "policyCode", "policy_code", "code", "policyId"),
          required(policy, "name", "title", "description"),
          PolicyVocabulary.canonicalRegion(required(policy, "jurisdiction", "region", "jurisdictionCode")),
          PolicyVocabulary.canonicalDataClass(required(policy, "dataClass", "data_class", "dataclass", "class")),
          regionSet(findValue(policy, "allowedRegions", "allowed_regions", "allowed"), "allowedRegions"),
          regionSet(findValue(policy, "deniedRegions", "denied_regions", "denied", "blocked"), "deniedRegions"),
          status(policy));
      validate(compiled);
      result.add(compiled);
    }
    return result;
  }

  private List<Map<?, ?>> parseAll(String content) {
    List<Map<?, ?>> list = new ArrayList<>();
    try {
      LoaderOptions loaderOptions = new LoaderOptions();
      loaderOptions.setCodePointLimit(1_048_576);
      Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
      
      for (Object doc : yaml.loadAll(content)) {
        if (doc instanceof Map<?, ?> root) {
          if (root.get("policy") instanceof Map<?, ?> p) {
            list.add(p);
          } else if (root.get("policies") instanceof Collection<?> col) {
            for (Object item : col) {
              if (item instanceof Map<?, ?> itemMap) list.add(itemMap);
            }
          } else if (isPolicyMap(root)) {
            list.add(root);
          } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "YAML must contain a 'policy' object, 'policies' list, or root policy attributes");
          }
        } else if (doc instanceof Collection<?> col) {
          for (Object item : col) {
            if (item instanceof Map<?, ?> itemMap && isPolicyMap(itemMap)) {
              list.add(itemMap);
            }
          }
        } else if (doc != null) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "YAML document must be a mapping or list of policy mappings");
        }
      }
    } catch (ApiException ae) {
      throw ae;
    } catch (Exception e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "malformed-policy", "Malformed policy YAML: " + e.getMessage());
    }
    return list;
  }

  private boolean isPolicyMap(Map<?, ?> map) {
    return map.containsKey("id") ||
           map.containsKey("policyCode") ||
           map.containsKey("policy_code") ||
           map.containsKey("jurisdiction") ||
           map.containsKey("dataClass") ||
           map.containsKey("allowedRegions");
  }

  private String required(Map<?, ?> policy, String... candidateKeys) {
    Object val = findValue(policy, candidateKeys);
    if (val == null || val.toString().isBlank()) {
      throw ApiException.unprocessable("Policy field '" + candidateKeys[0] + "' is required");
    }
    return val.toString().trim();
  }

  private Object findValue(Map<?, ?> map, String... candidateKeys) {
    for (String key : candidateKeys) {
      if (map.containsKey(key) && map.get(key) != null) {
        return map.get(key);
      }
    }
    return null;
  }

  private Set<String> regionSet(Object value, String field) {
    TreeSet<String> regions = new TreeSet<>();
    if (value == null) return regions;

    if (value instanceof Collection<?> collection) {
      for (Object item : collection) {
        if (item == null || item.toString().isBlank()) {
          throw ApiException.unprocessable("Policy field '" + field + "' contains a blank region");
        }
        regions.add(PolicyVocabulary.canonicalRegion(item.toString()));
      }
    } else if (value instanceof String str) {
      Arrays.stream(str.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(s -> regions.add(PolicyVocabulary.canonicalRegion(s)));
    } else {
      throw ApiException.unprocessable("Policy field '" + field + "' must be a list of regions");
    }
    return regions;
  }

  private PolicyStatus status(Map<?, ?> policy) {
    Object value = findValue(policy, "status", "state");
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
