package com.policymesh.lineage.dto;

public record LineageVerificationResponse(
        boolean valid,
        long recordsChecked,
        Long brokenAtRecord
) {
    public static LineageVerificationResponse valid(long recordsChecked) {
        return new LineageVerificationResponse(true, recordsChecked, null);
    }

    public static LineageVerificationResponse broken(long recordsChecked, long brokenAtRecord) {
        return new LineageVerificationResponse(false, recordsChecked, brokenAtRecord);
    }
}
