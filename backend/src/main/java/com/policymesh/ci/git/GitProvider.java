package com.policymesh.ci.git;

import com.policymesh.ci.CiDtos;
import java.util.List;

public interface GitProvider {
  /** Check if branch exists in the repository. */
  boolean branchExists(String branch);

  /** List available branches in repository. */
  List<String> listBranches();

  /**
   * Resolves and extracts real metadata for the given commit on the target branch.
   * Throws CiValidationException if branch doesn't exist, commit is not found, or commit doesn't belong to branch.
   */
  CommitInfo getCommit(String branch, String commitRef);

  /**
   * Reads raw file content from the git repository at the exact specified commit SHA.
   * Returns null if file does not exist at that commit.
   */
  String getFileContentAtCommit(String commitSha, String filePath);

  /**
   * Retrieves GitHub Actions check-runs and workflow run status for the given commit.
   */
  CiDtos.GitHubChecksSummary getGitHubChecks(String commitSha);

  /** Provider identifier for telemetry/audit logs (e.g. "LOCAL_GIT", "GITHUB_REST"). */
  String getProviderName();
}
