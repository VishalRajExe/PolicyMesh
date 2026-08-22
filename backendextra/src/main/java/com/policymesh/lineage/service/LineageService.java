package com.policymesh.lineage.service;

import com.policymesh.common.event.EventPublisherService;
import com.policymesh.config.KafkaConfig;
import com.policymesh.lineage.crypto.HashChainBuilder;
import com.policymesh.lineage.dto.LineageRecordResponse;
import com.policymesh.lineage.dto.LineageVerificationResponse;
import com.policymesh.lineage.entity.LineageRecord;
import com.policymesh.lineage.repository.LineageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only hash-chained lineage store. Every runtime/CI compliance
 * decision that needs audit evidence gets a LineageRecord whose
 * currentHash depends on the previous record's hash — tampering with any
 * historical record breaks the chain from that point forward, which
 * {@link #verifyChain()} detects.
 */
@Service
@RequiredArgsConstructor
public class LineageService {

    private final LineageRecordRepository lineageRecordRepository;
    private final HashChainBuilder hashChainBuilder;
    private final EventPublisherService eventPublisherService;

    /**
     * Appends a new lineage record for the given decision. Synchronized to
     * keep sequence-number allocation and chain-linking atomic under
     * concurrent enforcement requests (fine for a modular monolith; a
     * distributed lock would be the microservice evolution).
     */
    @Transactional
    public synchronized LineageRecord appendRecord(UUID decisionId) {
        Optional<LineageRecord> last = lineageRecordRepository.findTopByOrderBySequenceNoDesc();
        long nextSequence = last.map(r -> r.getSequenceNo() + 1).orElse(1L);
        String previousHash = last.map(LineageRecord::getCurrentHash).orElse(null);
        Instant now = Instant.now();

        String currentHash = hashChainBuilder.computeHash(previousHash, decisionId, nextSequence, now);

        LineageRecord record = LineageRecord.builder()
                .decisionId(decisionId)
                .sequenceNo(nextSequence)
                .previousHash(previousHash)
                .currentHash(currentHash)
                .timestamp(now)
                .build();

        LineageRecord saved = lineageRecordRepository.save(record);
        eventPublisherService.publish(KafkaConfig.TOPIC_LINEAGE_CREATED, saved.getId().toString(),
                Map.of("decisionId", decisionId.toString(), "sequenceNo", nextSequence, "hash", currentHash));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LineageRecordResponse> findAll() {
        return lineageRecordRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getSequenceNo(), b.getSequenceNo()))
                .map(LineageRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LineageRecordResponse> findById(UUID id) {
        return lineageRecordRepository.findById(id).map(LineageRecordResponse::from);
    }

    /**
     * Walks the entire chain in sequence order, recomputing each hash from
     * its stored fields and comparing it to the persisted currentHash, and
     * confirming previousHash correctly links to the prior record.
     */
    @Transactional(readOnly = true)
    public LineageVerificationResponse verifyChain() {
        List<LineageRecord> records = lineageRecordRepository.findAllByOrderBySequenceNoAsc().toList();

        String expectedPrevious = null;
        long checked = 0;

        for (LineageRecord record : records) {
            checked++;

            if (!java.util.Objects.equals(expectedPrevious, record.getPreviousHash())) {
                return LineageVerificationResponse.broken(checked, record.getSequenceNo());
            }

            String recomputed = hashChainBuilder.computeHash(
                    record.getPreviousHash(), record.getDecisionId(), record.getSequenceNo(), record.getTimestamp());

            if (!recomputed.equals(record.getCurrentHash())) {
                return LineageVerificationResponse.broken(checked, record.getSequenceNo());
            }

            expectedPrevious = record.getCurrentHash();
        }

        return LineageVerificationResponse.valid(checked);
    }
}
