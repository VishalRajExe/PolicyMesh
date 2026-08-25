package com.policymesh.github;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredRepositoryRepository extends JpaRepository<MonitoredRepository, Long> {

  List<MonitoredRepository> findByUserIdOrderByRepoFullNameAsc(Long userId);

  Optional<MonitoredRepository> findByUserIdAndGithubRepoId(Long userId, Long githubRepoId);

  Optional<MonitoredRepository> findByUserIdAndRepoFullNameIgnoreCase(Long userId, String repoFullName);

  List<MonitoredRepository> findByRepoFullNameIgnoreCaseAndIsMonitoredTrue(String repoFullName);

  void deleteByUserId(Long userId);
}