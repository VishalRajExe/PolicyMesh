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
    if (lower.contains("policy") || lower.contains("policies/") || lower.endsWith(".rego") || (lower.endsWith(".yaml") && !lower.contains(".github/")) || (lower.endsWith(".yml") && !lower.contains(".github/"))) {
      return ChangedFileCategory.POLICY;
    }
    if (lower.contains("dataflow") || lower.contains("dataflows/") || lower.contains("flow") || lower.contains("flows.json")) {
      return ChangedFileCategory.DATAFLOW;
    }
    if (lower.contains("service") || lower.contains("services/") || lower.contains("services.json") || lower.contains("topology")) {
      return ChangedFileCategory.SERVICE;
    }
    if (lower.endsWith(".md") || lower.contains("docs/") || lower.endsWith(".txt") || lower.contains("license")) {
      return ChangedFileCategory.DOCUMENTATION;
    }
    if (lower.endsWith(".json") || lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".properties") || lower.contains("config") || lower.contains(".github/")) {
      return ChangedFileCategory.CONFIG;
    }
    return ChangedFileCategory.CODE;
  }
}
