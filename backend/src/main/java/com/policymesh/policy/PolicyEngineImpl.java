package com.policymesh.policy;

import com.policymesh.compiler.CompiledPolicy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class PolicyEngineImpl implements PolicyEngine {
  private final PolicyRepository repo;
  private final PolicyCache cache;

  public PolicyEngineImpl(PolicyRepository repo, PolicyCache cache) {
    this.repo = repo;
    this.cache = cache;
  }

  @Override
  public PolicyEvaluation evaluate(String sourceService, String destinationService,
                                   String sourceRegion, String destinationRegion,
                                   String dataClass, Collection<String> tags) {
    String jurisdiction = PolicyVocabulary.canonicalRegion(sourceRegion);
    String data = PolicyVocabulary.canonicalDataClass(dataClass);
    List<CompiledPolicy> applicable = cache.applicable(jurisdiction, data, () -> loadAll(jurisdiction, data));
    return PolicyRuleEvaluator.evaluate(applicable, sourceRegion, destinationRegion, dataClass);
  }

  private List<CompiledPolicy> loadAll(String jurisdiction, String dataClass) {
    // The cached set for key (jurisdiction, dataClass) is the ACTIVE policies of that data
    // class whose jurisdiction is either the source region or GLOBAL (docs/REDIS.md key format).
    return PolicyRuleEvaluator.applicable(
        repo.findByDataClassIgnoreCaseAndStatus(dataClass, PolicyStatus.ACTIVE).stream()
            .map(CompiledPolicy::from)
            .toList(),
        jurisdiction);
  }
}
