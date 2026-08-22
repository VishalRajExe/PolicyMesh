package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the policy engine cannot complete an evaluation. */
public class PolicyEvaluationException extends PolicyMeshException {
    public PolicyEvaluationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "https://policymesh/errors/evaluation-failed");
    }
}
