package com.policymesh.dashboard.service;

import java.util.List;

public record DashboardSummaryResponse(
        int complianceScore,
        long totalPolicies,
        long totalServices,
        long allowedTransfers,
        long blockedTransfers,
        long activeViolations,
        List<RecentDecision> recentDecisions
) {
    public record RecentDecision(
            String sourceService,
            String destinationService,
            String decision,
            String reason,
            String timestamp
    ) {
    }
}
