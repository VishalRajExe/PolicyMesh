package com.policymesh.enforcement.service;

import com.policymesh.common.event.EventPublisherService;
import com.policymesh.config.KafkaConfig;
import com.policymesh.enforcement.dto.EnforcementCheckRequest;
import com.policymesh.enforcement.dto.EnforcementCheckResponse;
import com.policymesh.enforcement.engine.PolicyDecisionResult;
import com.policymesh.enforcement.engine.PolicyEngine;
import com.policymesh.enforcement.entity.Decision;
import com.policymesh.enforcement.entity.DecisionType;
import com.policymesh.enforcement.repository.DecisionRepository;
import com.policymesh.lineage.entity.LineageRecord;
import com.policymesh.lineage.service.LineageService;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Orchestrates a single runtime enforcement request:
 *  1. Evaluate the (possibly multiple) data-class tags through the PolicyEngine.
 *  2. Persist the resulting Decision.
 *  3. Append a hash-chained LineageRecord for audit evidence.
 *
 * If any evaluated tag results in DENY, the overall decision is DENY
 * (deny-wins semantics), matching the spirit of "the strictest applicable
 * rule governs".
 */
@Service
@RequiredArgsConstructor
public class EnforcementService {

    private final PolicyEngine policyEngine;
    private final DecisionRepository decisionRepository;
    private final LineageService lineageService;
    private final ServiceNodeRepository serviceNodeRepository;
    private final EventPublisherService eventPublisherService;

    @Transactional
    public EnforcementCheckResponse check(EnforcementCheckRequest request) {
        PolicyDecisionResult worst = null;

        for (String dataClass : request.dataClassTags()) {
            PolicyDecisionResult result = policyEngine.evaluate(
                    request.sourceRegion(), request.destinationRegion(), dataClass,
                    request.sourceService(), request.destinationService());

            if (worst == null || outranks(result.decision(), worst.decision())) {
                worst = result;
            }
        }

        // dataClassTags is validated @NotEmpty, so worst is never null here.
        DecisionType decisionType = DecisionType.valueOf(worst.decision().name());

        Decision decision = Decision.builder()
                .sourceServiceId(serviceNodeRepository.findByName(request.sourceService()).map(s -> s.getId()).orElse(null))
                .destinationServiceId(serviceNodeRepository.findByName(request.destinationService()).map(s -> s.getId()).orElse(null))
                .sourceServiceName(request.sourceService())
                .destinationServiceName(request.destinationService())
                .sourceRegion(request.sourceRegion().toUpperCase())
                .destinationRegion(request.destinationRegion().toUpperCase())
                .dataClass(String.join(",", request.dataClassTags()))
                .decision(decisionType)
                .reason(worst.reason())
                .policyCode(worst.policyId())
                .build();

        Decision savedDecision = decisionRepository.save(decision);

        LineageRecord lineageRecord = lineageService.appendRecord(savedDecision.getId());

        eventPublisherService.publish(KafkaConfig.TOPIC_DECISION_CREATED, savedDecision.getId().toString(),
                Map.of("decision", decisionType.name(), "policyCode", String.valueOf(worst.policyId())));

        return new EnforcementCheckResponse(
                decisionType.name(),
                worst.policyId(),
                worst.reason(),
                lineageRecord.getCurrentHash());
    }

    /** DENY > REROUTE > ALLOW, so the strictest applicable decision wins across multiple data-class tags. */
    private boolean outranks(PolicyDecisionResult.Decision candidate, PolicyDecisionResult.Decision current) {
        return rank(candidate) > rank(current);
    }

    private int rank(PolicyDecisionResult.Decision decision) {
        return switch (decision) {
            case DENY -> 2;
            case REROUTE -> 1;
            case ALLOW -> 0;
        };
    }
}
