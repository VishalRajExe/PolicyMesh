package com.policymesh.ai.dto;

import java.util.List;

public record ClassifyResponse(List<FieldClassification> classifications) {
}
