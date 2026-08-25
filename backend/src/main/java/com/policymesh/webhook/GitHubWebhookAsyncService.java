package com.policymesh.webhook;

import com.policymesh.ci.CiDtos;
import com.policymesh.ci.CiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
public class GitHubWebhookAsyncService {
  private static final Logger log = LoggerFactory.getLogger(GitHubWebhookAsyncService.class);

  private final CiService ciService;
  private final WebhookDeliveryRepository deliveryRepository;
  private final com.policymesh.github.MonitoredRepositoryRepository monitoredRepository;

  public GitHubWebhookAsyncService(
      CiService ciService,
      WebhookDeliveryRepository deliveryRepository,
      com.policymesh.github.MonitoredRepositoryRepository monitoredRepository
  ) {
    this.ciService = ciService;
    this.deliveryRepository = deliveryRepository;
    this.monitoredRepository = monitoredRepository;
  }

  @Async
  public CompletableFuture<Void> processPushEventAsync(String deliveryId, String branch, String commitSha) {
    log.info("Starting asynchronous policy compliance analysis for webhook delivery: {} (commit: {}, branch: {})",
        deliveryId, commitSha, branch);

    try {
      CiDtos.Response response = ciService.run(branch, commitSha);
      updateDeliverySuccess(deliveryId, response);
      log.info("Completed compliance scan for delivery: {} -> Result: {}, Merge Decision: {}",
          deliveryId, response.status(), response.finalDecision() != null ? response.finalDecision().decision() : "N/A");
    } catch (Exception e) {
      log.error("Failed processing push event for delivery {}: {}", deliveryId, e.getMessage(), e);
      updateDeliveryFailure(deliveryId, e.getMessage());
    }

    return CompletableFuture.completedFuture(null);
  }

  @Transactional
  public void updateDeliverySuccess(String deliveryId, CiDtos.Response response) {
    deliveryRepository.findByDeliveryId(deliveryId).ifPresent(delivery -> {
      delivery.setStatus("COMPLETED");
      delivery.setScanId(response.id());
      delivery.setSummary("Status: " + response.status() + " | Violations: " + response.violations().size()
          + " | Merge: " + (response.finalDecision() != null ? response.finalDecision().decision() : "UNKNOWN"));
      delivery.setCompletedAt(Instant.now());
      deliveryRepository.save(delivery);

      if (delivery.getRepository() != null) {
        var repos = monitoredRepository.findByRepoFullNameIgnoreCaseAndIsMonitoredTrue(delivery.getRepository());
        for (var repo : repos) {
          repo.setLastCommitSha(response.commitHash());
          repo.setLastCommitMessage(response.commitMessage());
          repo.setLastScanStatus(response.status());
          repo.setLastScanId(response.id());
          repo.setLastScanTime(Instant.now());
          monitoredRepository.save(repo);
        }
      }
    });
  }

  @Transactional
  public void updateDeliveryFailure(String deliveryId, String errorMessage) {
    deliveryRepository.findByDeliveryId(deliveryId).ifPresent(delivery -> {
      delivery.setStatus("FAILED");
      delivery.setErrorMessage(errorMessage != null && errorMessage.length() > 1900 ? errorMessage.substring(0, 1900) : errorMessage);
      delivery.setCompletedAt(Instant.now());
      deliveryRepository.save(delivery);
    });
  }
}