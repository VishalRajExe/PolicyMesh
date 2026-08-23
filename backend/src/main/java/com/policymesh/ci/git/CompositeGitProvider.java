package com.policymesh.ci.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Primary
public class CompositeGitProvider implements GitProvider {
  private static final Logger log = LoggerFactory.getLogger(CompositeGitProvider.class);
  private final GitHubProvider gitHubProvider;
  private final LocalGitProvider localGitProvider;

  public CompositeGitProvider(GitHubProvider gitHubProvider, LocalGitProvider localGitProvider) {
    this.gitHubProvider = gitHubProvider;
    this.localGitProvider = localGitProvider;
  }

  private GitProvider activeProvider() {
    if (gitHubProvider.isConfigured()) {
      return gitHubProvider;
    }
    return localGitProvider;
  }

  @Override
  public String getProviderName() {
    return activeProvider().getProviderName();
  }

  @Override
  public boolean branchExists(String branch) {
    if (gitHubProvider.isConfigured()) {
      return gitHubProvider.branchExists(branch);
    }
    return localGitProvider.branchExists(branch);
  }

  @Override
  public List<String> listBranches() {
    Set<String> set = new LinkedHashSet<>();
    if (gitHubProvider.isConfigured()) {
      set.addAll(gitHubProvider.listBranches());
    }
    set.addAll(localGitProvider.listBranches());
    if (set.isEmpty()) {
      set.add("main");
      set.add("develop");
      set.add("staging");
    }
    return List.copyOf(set);
  }

  @Override
  public String getFileContentAtCommit(String commitSha, String filePath) {
    GitProvider provider = activeProvider();
    String content = provider.getFileContentAtCommit(commitSha, filePath);
    if (content == null && provider == gitHubProvider) {
      content = localGitProvider.getFileContentAtCommit(commitSha, filePath);
    }
    return content;
  }

  @Override
  public CommitInfo getCommit(String branch, String commitRef) {
    GitProvider provider = activeProvider();
    log.info("Resolving commit '{}' on branch '{}' using provider: {}", commitRef, branch, provider.getProviderName());
    try {
      return provider.getCommit(branch, commitRef);
    } catch (CiValidationException e) {
      // If GitHub is configured but fails, fall back to local provider
      if (provider == gitHubProvider) {
        log.warn("GitHub provider failed ({}); trying LocalGitProvider fallback", e.getMessage());
        return localGitProvider.getCommit(branch, commitRef);
      }
      throw e;
    }
  }
}
