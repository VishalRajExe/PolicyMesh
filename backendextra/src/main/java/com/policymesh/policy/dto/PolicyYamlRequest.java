package com.policymesh.policy.dto;

import jakarta.validation.constraints.NotBlank;

/** Alternate creation path: submit a raw policy YAML document to be parsed + compiled + persisted. */
public record PolicyYamlRequest(
        @NotBlank(message = "yaml content is required") String yaml
) {
}
