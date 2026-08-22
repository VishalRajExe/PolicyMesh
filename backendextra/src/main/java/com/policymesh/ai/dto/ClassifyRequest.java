package com.policymesh.ai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ClassifyRequest(
        @NotEmpty(message = "fields must contain at least one entry") List<FieldClassifyRequest> fields
) {
}
