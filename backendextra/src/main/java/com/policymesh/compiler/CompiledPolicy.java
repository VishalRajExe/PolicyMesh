package com.policymesh.compiler;

import java.util.List;
import java.util.Set;

/**
 * Internal, immutable representation of a policy after parsing + validation
 * + compilation. This is the ONLY form the rest of the system (policy
 * engine, graph analyzer, CI checker) should depend on — nobody outside
 * the compiler package touches raw YAML.
 */
public record CompiledPolicy(
        String id,
        String name,
        String jurisdiction,
        String dataClass,
        Set<String> allowedRegions,
        Set<String> deniedRegions
) {
    public boolean isRegionAllowed(String region) {
        return allowedRegions.contains(region);
    }

    public boolean isRegionDenied(String region) {
        return deniedRegions.contains(region);
    }

    public List<String> allowedRegionsList() {
        return allowedRegions.stream().toList();
    }
}
