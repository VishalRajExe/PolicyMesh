package com.policymesh.policy;

import java.util.Collection;

/** The one authoritative evaluation path used by enforcement, graph analysis and CI. */
public interface PolicyEngine {
  PolicyEvaluation evaluate(String sourceService, String destinationService,
                            String sourceRegion, String destinationRegion,
                            String dataClass, Collection<String> tags);
}
