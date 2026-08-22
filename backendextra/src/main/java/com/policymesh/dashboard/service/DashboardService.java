package com.policymesh.dashboard.service;

import com.policymesh.enforcement.entity.DecisionType;
import com.policymesh.enforcement.repository.DecisionRepository;
import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.service.GraphService;
import com.policymesh.policy.entity.PolicyStatus;
import com.policymesh.policy.repository.PolicyRepository;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PolicyRepository policyRepository;
    private final ServiceNodeRepository serviceNodeRepository;
    private final DecisionRepository decisionRepository;
    private final GraphService graphService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalPolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        long totalServices = serviceNodeRepository.count();
        long allowed = decisionRepository.countByDecision(DecisionType.ALLOW);
        long blocked = decisionRepository.countByDecision(DecisionType.DENY) + decisionRepository.countByDecision(DecisionType.REROUTE);

        GraphCheckResult graphCheck = graphService.validate();
        long activeViolations = graphCheck.violations().size();

        long totalTransfers = allowed + blocked;
        int complianceScore = totalTransfers == 0 ? 100
                : (int) Math.round((allowed * 100.0) / totalTransfers);

        List<DashboardSummaryResponse.RecentDecision> recent = decisionRepository
                .findAllByOrderByTimestampDesc(PageRequest.of(0, 10)).stream()
                .map(d -> new DashboardSummaryResponse.RecentDecision(
                        d.getSourceServiceName(), d.getDestinationServiceName(),
                        d.getDecision().name(), d.getReason(), d.getTimestamp().toString()))
                .toList();

        return new DashboardSummaryResponse(complianceScore, totalPolicies, totalServices,
                allowed, blocked, activeViolations, recent);
    }
}
