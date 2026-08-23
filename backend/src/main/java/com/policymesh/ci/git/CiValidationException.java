package com.policymesh.ci.git;

public class CiValidationException extends RuntimeException {
  private final String errorCode;
  private final String branch;
  private final String commitHash;

  public CiValidationException(String errorCode, String message, String branch, String commitHash) {
    super(message);
    this.errorCode = errorCode;
    this.branch = branch;
    this.commitHash = commitHash;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getBranch() {
    return branch;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public static CiValidationException commitNotFound(String commitHash, String branch) {
    return new CiValidationException(
        "COMMIT_NOT_FOUND",
        "Commit '" + commitHash + "' could not be found in repository on branch '" + branch + "'.",
        branch,
        commitHash
    );
  }

  public static CiValidationException branchNotFound(String branch) {
    return new CiValidationException(
        "BRANCH_NOT_FOUND",
        "Target Git branch '" + branch + "' does not exist in the repository.",
        branch,
        null
    );
  }

  public static CiValidationException commitBranchMismatch(String commitHash, String branch) {
    return new CiValidationException(
        "COMMIT_BRANCH_MISMATCH",
        "Commit '" + commitHash + "' is not reachable from or part of branch '" + branch + "'.",
        branch,
        commitHash
    );
  }

  public static CiValidationException invalidSha(String commitHash) {
    return new CiValidationException(
        "INVALID_SHA_FORMAT",
        "Commit SHA '" + commitHash + "' has an invalid format. Must be a 3-40 character hexadecimal hash or 'HEAD'.",
        null,
        commitHash
    );
  }

  public static CiValidationException providerUnavailable(String details) {
    return new CiValidationException(
        "PROVIDER_UNAVAILABLE",
        "Git provider service is currently unavailable: " + details,
        null,
        null
    );
  }
}
