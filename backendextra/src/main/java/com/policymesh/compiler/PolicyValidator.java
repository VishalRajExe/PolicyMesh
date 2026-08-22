package com.policymesh.compiler;

import com.policymesh.common.exception.InvalidPolicyException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates a {@link ParsedPolicyDocument} before it is allowed to become
 * a {@link CompiledPolicy}. Catches malformed policies early with clear,
 * actionable messages.
 */
@Component
public class PolicyValidator {

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9\\-_.]{1,99}$");

    public void validate(ParsedPolicyDocument doc) {
        List<String> errors = new java.util.ArrayList<>();

        if (isBlank(doc.getId())) {
            errors.add("policy.id is required");
        } else if (!ID_PATTERN.matcher(doc.getId()).matches()) {
            errors.add("policy.id must be alphanumeric with optional '-', '_', '.' (got: " + doc.getId() + ")");
        }

        if (isBlank(doc.getName())) {
            errors.add("policy.name is required");
        }

        if (isBlank(doc.getJurisdiction())) {
            errors.add("policy.jurisdiction is required");
        }

        if (isBlank(doc.getDataClass())) {
            errors.add("policy.dataClass is required");
        }

        List<String> allowed = doc.getAllowedRegions() == null ? List.of() : doc.getAllowedRegions();
        List<String> denied = doc.getDeniedRegions() == null ? List.of() : doc.getDeniedRegions();

        if (allowed.isEmpty() && denied.isEmpty()) {
            errors.add("policy must declare at least one of allowedRegions or deniedRegions");
        }

        Set<String> overlap = new HashSet<>(allowed);
        overlap.retainAll(new HashSet<>(denied));
        if (!overlap.isEmpty()) {
            errors.add("policy cannot both allow and deny the same region(s): " + overlap);
        }

        for (String region : allowed) {
            if (isBlank(region)) {
                errors.add("allowedRegions contains a blank entry");
                break;
            }
        }
        for (String region : denied) {
            if (isBlank(region)) {
                errors.add("deniedRegions contains a blank entry");
                break;
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidPolicyException("Policy validation failed: " + String.join("; ", errors));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
