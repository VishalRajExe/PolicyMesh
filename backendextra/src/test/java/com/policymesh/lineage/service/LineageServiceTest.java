package com.policymesh.lineage.service;

import com.policymesh.enforcement.entity.Decision;
import com.policymesh.enforcement.entity.DecisionType;
import com.policymesh.enforcement.repository.DecisionRepository;
import com.policymesh.lineage.dto.LineageVerificationResponse;
import com.policymesh.lineage.entity.LineageRecord;
import com.policymesh.lineage.repository.LineageRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LineageServiceTest {

    @Autowired
    private LineageService lineageService;

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private LineageRecordRepository lineageRecordRepository;

    private Decision persistDummyDecision() {
        Decision decision = Decision.builder()
                .sourceServiceName("orders-api")
                .destinationServiceName("analytics-api")
                .sourceRegion("EU")
                .destinationRegion("US")
                .dataClass("PII")
                .decision(DecisionType.DENY)
                .reason("test")
                .build();
        return decisionRepository.save(decision);
    }

    @Test
    void freshChain_isValid() {
        for (int i = 0; i < 5; i++) {
            Decision d = persistDummyDecision();
            lineageService.appendRecord(d.getId());
        }

        LineageVerificationResponse result = lineageService.verifyChain();

        assertThat(result.valid()).isTrue();
        assertThat(result.recordsChecked()).isEqualTo(5);
    }

    @Test
    void chainLinksSequentially() {
        Decision d1 = persistDummyDecision();
        LineageRecord r1 = lineageService.appendRecord(d1.getId());
        Decision d2 = persistDummyDecision();
        LineageRecord r2 = lineageService.appendRecord(d2.getId());

        assertThat(r1.getPreviousHash()).isNull();
        assertThat(r2.getPreviousHash()).isEqualTo(r1.getCurrentHash());
        assertThat(r2.getSequenceNo()).isEqualTo(r1.getSequenceNo() + 1);
    }

    @Test
    void modifiedCurrentHash_breaksChain() {
        Decision d1 = persistDummyDecision();
        lineageService.appendRecord(d1.getId());
        Decision d2 = persistDummyDecision();
        lineageService.appendRecord(d2.getId());

        LineageRecord first = lineageRecordRepository.findAllByOrderBySequenceNoAsc().findFirst().orElseThrow();
        first.setCurrentHash("tampered-hash-value");
        lineageRecordRepository.save(first);

        LineageVerificationResponse result = lineageService.verifyChain();

        assertThat(result.valid()).isFalse();
        assertThat(result.brokenAtRecord()).isEqualTo(first.getSequenceNo());
    }

    @Test
    void modifiedPreviousHash_breaksChain() {
        Decision d1 = persistDummyDecision();
        lineageService.appendRecord(d1.getId());
        Decision d2 = persistDummyDecision();
        lineageService.appendRecord(d2.getId());

        LineageRecord second = lineageRecordRepository.findAllByOrderBySequenceNoAsc()
                .skip(1).findFirst().orElseThrow();
        second.setPreviousHash("bogus-previous-hash");
        lineageRecordRepository.save(second);

        LineageVerificationResponse result = lineageService.verifyChain();

        assertThat(result.valid()).isFalse();
    }
}
