package com.policymesh.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * RFC 7807 "application/problem+json" error body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetailResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        Map<String, String> errors
) {
    public static ProblemDetailResponse of(String type, String title, int status, String detail, String instance) {
        return new ProblemDetailResponse(type, title, status, detail, instance, Instant.now(), null);
    }

    public static ProblemDetailResponse withErrors(String type, String title, int status, String detail,
                                                     String instance, Map<String, String> errors) {
        return new ProblemDetailResponse(type, title, status, detail, instance, Instant.now(), errors);
    }
}
