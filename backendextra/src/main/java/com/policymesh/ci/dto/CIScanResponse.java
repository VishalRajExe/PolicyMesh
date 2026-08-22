package com.policymesh.ci.dto;

import com.policymesh.ci.entity.CIScan;
import com.policymesh.graph.model.GraphCheckResult;

import java.time.Instant;
import java.util.UUID;

public record CIScanResponse(
        UUID id,
        String commitHash,
        String branch,
        String status,
        int violationCount,
        GraphCheckResult result,
        Instant startedAt,
        Instant completedAt
) {
}
