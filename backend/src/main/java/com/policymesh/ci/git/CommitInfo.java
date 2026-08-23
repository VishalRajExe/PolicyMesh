package com.policymesh.ci.git;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommitInfo(
    String fullSha,
    String shortSha,
    String branch,
    String authorName,
    String authorEmail,
    String message,
    Instant timestamp,
    String parentSha,
    List<ChangedFile> changedFiles
) {
  public String shortParentSha() {
    if (parentSha == null || parentSha.isBlank()) return null;
    return parentSha.length() > 7 ? parentSha.substring(0, 7) : parentSha;
  }
}
