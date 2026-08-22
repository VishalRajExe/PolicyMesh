package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the lineage hash chain fails verification during a write operation. */
public class LineageIntegrityException extends PolicyMeshException {
    public LineageIntegrityException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "https://policymesh/errors/lineage-integrity");
    }
}
