package com.policymesh.policy.validator;

import com.policymesh.common.exception.InvalidPolicyException;
import com.policymesh.policy.dto.PolicyRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Additional semantic checks on inbound {@link PolicyRequest} objects that
 * go beyond simple bean validation (e.g. cross-field consistency).
 */
@Component
public class PolicyRequestValidator {

    public void validate(PolicyRequest request) {
        List<String> allowed = request.allowedRegions() == null ? List.of() : request.allowedRegions();
        List<String> denied = request.deniedRegions() == null ? List.of() : request.deniedRegions();

        if (allowed.isEmpty() && denied.isEmpty()) {
            throw new InvalidPolicyException("At least one of allowedRegions or deniedRegions must be provided");
        }

        Set<String> overlap = new HashSet<>(upper(allowed));
        overlap.retainAll(new HashSet<>(upper(denied)));
        if (!overlap.isEmpty()) {
            throw new InvalidPolicyException("A region cannot be both allowed and denied: " + overlap);
        }
    }

    private List<String> upper(List<String> regions) {
        return regions.stream().map(String::toUpperCase).toList();
    }
}
