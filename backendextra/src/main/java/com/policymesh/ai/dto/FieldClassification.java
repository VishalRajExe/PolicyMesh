package com.policymesh.ai.dto;

import java.util.UUID;

public record FieldClassification(
        UUID id,
        String field,
        String classification,
        double confidence,
        boolean approved
) {
}
