package com.policymesh.webhook;

import com.policymesh.ci.CIScan;
import com.policymesh.ci.CIScanRepository;
import com.policymesh.ci.CiDtos;
import com.policymesh.ci.CiService;
import com.policymesh.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubApiController {

  private final WebhookDeliveryRepository deliveryRepository;
  private final CIScanRepository scanRepository;
  private final CiService ciService;

  public GitHubApiController(
      WebhookDeliveryRepository deliveryRepository,
      CIScanRepository scanRepository,
      CiService ciService
  ) {
    this.deliveryRepository = deliveryRepository;
    this.scanRepository = scanRepository;
    this.ciService = ciService;
  }

  @GetMapping("/webhook-deliveries")
  public ResponseEntity<Page<WebhookDelivery>> listDeliveries(Pageable pageable) {
    return ResponseEntity.ok(deliveryRepository.findAllByOrderByReceivedAtDesc(pageable));
  }

  @GetMapping("/commits")
  public ResponseEntity<Page<CiDtos.Response>> listCommits(Pageable pageable) {
    return ResponseEntity.ok(ciService.listScans(pageable));
  }

  @GetMapping("/commits/{sha}")
  public ResponseEntity<CiDtos.Response> getCommit(@PathVariable String sha) {
    List<CIScan> list = scanRepository.findByCommitHashIgnoreCaseOrderByStartedAtDesc(sha);
    if (list.isEmpty()) {
      // Try resolving via CI service directly
      try {
        CiDtos.Response res = ciService.run("main", sha);
        return ResponseEntity.ok(res);
      } catch (Exception e) {
        throw ApiException.notFound("Commit " + sha + " not found or has not been analyzed.");
      }
    }
    return ResponseEntity.ok(ciService.one(list.get(0).getId()));
  }

  @GetMapping("/commits/{sha}/violations")
  public ResponseEntity<Map<String, Object>> getCommitViolations(@PathVariable String sha) {
    List<CIScan> list = scanRepository.findByCommitHashIgnoreCaseOrderByStartedAtDesc(sha);
    if (list.isEmpty()) {
      throw ApiException.notFound("No compliance scan found for commit " + sha);
    }
    CiDtos.Response res = ciService.one(list.get(0).getId());
    return ResponseEntity.ok(Map.of(
        "commitHash", res.commitHash(),
        "branch", res.branch(),
        "status", res.status(),
        "violationCount", res.violations().size(),
        "violations", res.violations(),
        "finalDecision", res.finalDecision()
    ));
  }
}