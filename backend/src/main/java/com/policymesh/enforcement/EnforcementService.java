package com.policymesh.enforcement;

import com.policymesh.common.ApiException;
import com.policymesh.events.EventPublisher;
import com.policymesh.lineage.LineageService;
import com.policymesh.policy.PolicyEngine;
import com.policymesh.policy.PolicyEvaluation;
import com.policymesh.policy.PolicyRuleEvaluator;
import com.policymesh.policy.PolicyVocabulary;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Runtime enforcement check. Every request is evaluated by the one PolicyEngine, persisted as a
 * DecisionRecord, appended to the lineage chain and (best-effort) announced on Kafka.
 */
@Service
@Transactional
public class EnforcementService {
  private final PolicyEngine engine;
  private final LineageService lineage;
  private final DecisionRepository decisions;
  private final ServiceNodeRepository services;
  private final EventPublisher events;

  public EnforcementService(PolicyEngine engine, LineageService lineage, DecisionRepository decisions,
                            ServiceNodeRepository services, EventPublisher events) {
    this.engine = engine;
    this.lineage = lineage;
    this.decisions = decisions;
    this.services = services;
    this.events = events;
  }

  public EnforcementDtos.Response check(EnforcementDtos.Request r) {
    String source = requireName(r.effectiveSource(), "sourceService");
    String destination = requireName(r.effectiveDestination(), "destinationService");
    String sourceRegion = resolveRegion(r.effectiveSourceRegion(), source, "sourceRegion");
    String destinationRegion = resolveRegion(r.effectiveDestinationRegion(), destination, "destinationRegion");
    Set<String> dataClasses = PolicyRuleEvaluator.canonicalDataClasses(r.effectiveTags());
    if (dataClasses.isEmpty()) {
      throw ApiException.unprocessable("dataClassTags must contain at least one data class");
    }

    List<PolicyEvaluation> evaluations = new ArrayList<>();
    for (String dataClass : new TreeSet<>(dataClasses)) {
      evaluations.add(engine.evaluate(source, destination, sourceRegion, destinationRegion, dataClass, dataClasses));
    }
    PolicyEvaluation worst = PolicyRuleEvaluator.worstOf(evaluations);
    String reason = dataClasses.size() == 1 ? worst.reason()
        : worst.reason() + " (deciding data class evaluated among: " + String.join(", ", new TreeSet<>(dataClasses)) + ")";

    DecisionRecord record = new DecisionRecord();
    record.setSourceService(source);
    record.setDestinationService(destination);
    record.setSourceRegion(sourceRegion);
    record.setDestinationRegion(destinationRegion);
    record.setDataClass(String.join(",", new TreeSet<>(dataClasses)));
    record.setDecision(worst.decision().name());
    record.setPolicyId(worst.policyId());
    record.setReason(reason);
    record = decisions.save(record);

    var lineageRecord = lineage.append(record);

    Map<String, Object> payload = new HashMap<>();
    payload.put("decisionId", record.getId());
    payload.put("decision", record.getDecision());
    payload.put("dataClass", record.getDataClass());
    payload.put("source", source);
    payload.put("destination", destination);
    events.publish(EventPublisher.TOPIC_DECISION_CREATED, payload);

    return new EnforcementDtos.Response(worst.decision().name(), worst.policyId(), reason,
        record.getId(), lineageRecord.id(), lineageRecord.currentHash());
  }

  private String requireName(String value, String field) {
    if (value == null || value.isBlank()) {
      throw ApiException.unprocessable("'" + field + "' is required");
    }
    return value.trim();
  }

  private String resolveRegion(String region, String serviceName, String field) {
    if (region != null && !region.isBlank()) {
      return PolicyVocabulary.canonicalRegion(region);
    }
    return services.findByNameIgnoreCase(serviceName.trim())
        .orElseThrow(() -> ApiException.unprocessable(
            "'" + field + "' is required: service '" + serviceName + "' is not registered, so its region cannot be resolved"))
        .getRegion();
  }
}
