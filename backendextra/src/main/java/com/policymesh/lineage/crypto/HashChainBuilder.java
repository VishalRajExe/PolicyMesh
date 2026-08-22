package com.policymesh.lineage.crypto;

import com.policymesh.common.util.HashUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds SHA-256 hash-chain links for lineage records.
 *
 * currentHash = SHA256(previousHash + decisionId + sequenceNo + timestamp)
 *
 * Cryptographic signing (e.g. Ed25519/RSA over currentHash) can be added
 * later without changing this hashing scheme — the {@code signature} field
 * on LineageRecord is reserved for that purpose and intentionally left
 * unset here. No fake/simulated signatures are ever produced.
 */
@Component
public class HashChainBuilder {

    public String computeHash(String previousHash, UUID decisionId, long sequenceNo, Instant timestamp) {
        String basis = (previousHash == null ? "GENESIS" : previousHash)
                + "|" + decisionId
                + "|" + sequenceNo
                + "|" + timestamp.toString();
        return HashUtil.sha256(basis);
    }
}
