package com.policymesh.ci.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.dto.CIScanRequest;
import com.policymesh.ci.dto.CIScanResponse;
import com.policymesh.ci.entity.CIScan;
import com.policymesh.ci.entity.CIScanStatus;
import com.policymesh.ci.repository.CIScanRepository;
import com.policymesh.common.event.EventPublisherService;
import com.policymesh.common.exception.ResourceNotFoundException;
import com.policymesh.config.KafkaConfig;
import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.model.GraphCheckStatus;
import com.policymesh.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Runs a PolicyMesh compliance scan (build-time graph validation),
 * persists a {@link CIScan} record for later retrieval, and publishes a
 * Kafka event. This service backs both the REST endpoint
 * (POST /api/v1/ci/check) and the standalone CLI checker
 * ({@link com.policymesh.ci.analyzer.CIComplianceChecker}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CIScanService {

    private final GraphService graphService;
    private final CIScanRepository ciScanRepository;
    private final EventPublisherService eventPublisherService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CIScanResponse runScan(CIScanRequest request) {
        Instant started = Instant.now();
        GraphCheckResult result = graphService.validate();
        Instant completed = Instant.now();

        CIScanStatus status = result.status() == GraphCheckStatus.PASSED ? CIScanStatus.PASSED : CIScanStatus.FAILED;

        String reportJson;
        try {
            reportJson = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            reportJson = "{}";
        }

        CIScan scan = CIScan.builder()
                .commitHash(request == null ? null : request.commitHash())
                .branch(request == null ? null : request.branch())
                .status(status)
                .violationCount(result.violations().size())
                .reportJson(reportJson)
                .startedAt(started)
                .completedAt(completed)
                .build();

        CIScan saved = ciScanRepository.save(scan);

        eventPublisherService.publish(KafkaConfig.TOPIC_CI_COMPLETED, saved.getId().toString(),
                Map.of("status", status.name(), "violationCount", result.violations().size()));

        return new CIScanResponse(saved.getId(), saved.getCommitHash(), saved.getBranch(),
                saved.getStatus().name(), saved.getViolationCount(), result, saved.getStartedAt(), saved.getCompletedAt());
    }

    @Transactional(readOnly = true)
    public CIScanResponse findById(UUID id) {
        CIScan scan = ciScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CI scan not found: " + id));
        GraphCheckResult result = null;
        try {
            result = objectMapper.readValue(scan.getReportJson(), GraphCheckResult.class);
        } catch (Exception e) {
            log.warn("Could not deserialize stored CI report for scan {}", id);
        }
        return new CIScanResponse(scan.getId(), scan.getCommitHash(), scan.getBranch(), scan.getStatus().name(),
                scan.getViolationCount(), result, scan.getStartedAt(), scan.getCompletedAt());
    }
}
