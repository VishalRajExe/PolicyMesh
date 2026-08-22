package com.policymesh.lineage.dto;

import com.policymesh.lineage.entity.LineageRecord;
import com.policymesh.lineage.entity.LineageStatus;

import java.time.Instant;
import java.util.UUID;

public record LineageRecordResponse(
        UUID id,
        UUID decisionId,
        long sequenceNo,
        String previousHash,
        String currentHash,
        String signature,
        LineageStatus status,
        Instant timestamp
) {
    public static LineageRecordResponse from(LineageRecord r) {
        return new LineageRecordResponse(r.getId(), r.getDecisionId(), r.getSequenceNo(), r.getPreviousHash(),
                r.getCurrentHash(), r.getSignature(), r.getStatus(), r.getTimestamp());
    }
}
