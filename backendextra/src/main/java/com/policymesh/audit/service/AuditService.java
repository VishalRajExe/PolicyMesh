package com.policymesh.audit.service;

import com.policymesh.enforcement.repository.DecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Read-only audit trail view. Kept intentionally thin — the durable audit
 * evidence lives in the lineage hash chain (see lineage module); this
 * service just provides a convenient combined view for auditors.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final DecisionRepository decisionRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recentActivity(int limit) {
        return decisionRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(d -> Map.<String, Object>of(
                        "decisionId", d.getId(),
                        "sourceService", d.getSourceServiceName(),
                        "destinationService", d.getDestinationServiceName(),
                        "sourceRegion", d.getSourceRegion(),
                        "destinationRegion", d.getDestinationRegion(),
                        "dataClass", d.getDataClass(),
                        "decision", d.getDecision().name(),
                        "reason", d.getReason() == null ? "" : d.getReason(),
                        "timestamp", d.getTimestamp().toString()
                ))
                .toList();
    }
}
