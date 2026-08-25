package com.policymesh.ci.git;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangedFile(
    String path,
    String status, // ADDED, MODIFIED, DELETED
    ChangedFileCategory category,
    String patch
) {
  public static ChangedFile of(String path, String status) {
    return new ChangedFile(path, status, categorize(path), null);
  }

  public static ChangedFile of(String path, String status, String patch) {
    return new ChangedFile(path, status, categorize(path), patch);
  }

  public static ChangedFileCategory categorize(String path) {
    if (path == null) return ChangedFileCategory.CODE;
    String lower = path.toLowerCase().replace('\\', '/');

    // 1. Source code files (.java, .py, .js, .jsx, etc.) are always CODE
    if (lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js") || lower.endsWith(".jsx") ||
        lower.endsWith(".ts") || lower.endsWith(".tsx") || lower.endsWith(".go") || lower.endsWith(".rs") ||
        lower.endsWith(".c") || lower.endsWith(".cpp") || lower.endsWith(".html") || lower.endsWith(".css")) {
      return ChangedFileCategory.CODE;
    }

    // 2. Policy governance definitions (YAML or Rego files in policies/ directory or policy configs)
    if (lower.startsWith("policies/") || lower.contains("/policies/") || lower.endsWith(".rego") ||
        ((lower.endsWith(".yaml") || lower.endsWith(".yml")) && !lower.contains(".github/") && (lower.contains("policy") || lower.contains("policies")))) {
      return ChangedFileCategory.POLICY;
    }

    // 3. Topology & Data flow definitions
    if (lower.contains("dataflow") || lower.contains("dataflows/") || lower.endsWith("flow.json") || lower.endsWith("flows.json")) {
      return ChangedFileCategory.DATAFLOW;
    }
    if (lower.contains("service-graph") || lower.endsWith("services.json") || lower.contains("topology.json")) {
      return ChangedFileCategory.SERVICE;
    }

    // 4. Documentation
    if (lower.endsWith(".md") || lower.startsWith("docs/") || lower.contains("/docs/") || lower.endsWith(".txt") || lower.contains("license")) {
      return ChangedFileCategory.DOCUMENTATION;
    }

    // 5. Config / Migration files
    if (lower.endsWith(".json") || lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".properties") || lower.endsWith(".sql") || lower.contains(".github/")) {
      return ChangedFileCategory.CONFIG;
    }

    return ChangedFileCategory.CODE;
  }
}
