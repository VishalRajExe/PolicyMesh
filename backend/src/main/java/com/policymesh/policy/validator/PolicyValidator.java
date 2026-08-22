package com.policymesh.policy.validator;

import com.policymesh.policy.dto.PolicyRequest;
    import org.springframework.stereotype.Component;
    import org.springframework.validation.Errors;
    import org.springframework.validation.Validator;

@Component
public class PolicyValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PolicyRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PolicyRequest request = (PolicyRequest) target;

        // Validate that allowedRegions and deniedRegions don't overlap
        if (request.getAllowedRegions() != null && request.getDeniedRegions() != null) {
            for (String allowedRegion : request.getAllowedRegions()) {
                if (request.getDeniedRegions().contains(allowedRegion)) {
                    errors.rejectValue("deniedRegions", "policy.regions.overlap",
                            "Region cannot be both allowed and denied: " + allowedRegion);
                    break;
                }
            }
        }

        // Validate that at least one region is specified
        boolean hasAllowed = request.getAllowedRegions() != null && !request.getAllowedRegions().isEmpty();
        boolean hasDenied = request.getDeniedRegions() != null && !request.getDeniedRegions().isEmpty();
        if (!hasAllowed && !hasDenied) {
            errors.rejectValue("allowedRegions", "policy.regions.required",
                    "At least one allowed or denied region must be specified");
        }
    }
}