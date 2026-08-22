package com.policymesh.lineage.entity;

/** Signing status. SIGNED is reserved for when cryptographic signing is added later. */
public enum LineageStatus {
    UNSIGNED,
    SIGNED,
    INVALID
}
