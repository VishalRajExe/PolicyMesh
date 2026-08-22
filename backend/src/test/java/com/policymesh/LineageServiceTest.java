package com.policymesh;

import com.policymesh.enforcement.DecisionRecord;
import com.policymesh.lineage.LineageDtos;
import com.policymesh.lineage.LineageRecord;
import com.policymesh.lineage.LineageRepository;
import com.policymesh.lineage.LineageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:lineage;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "policymesh.kafka.enabled=false", "policymesh.redis.enabled=false"})
@Transactional
class LineageServiceTest {
  @Autowired LineageService service;
  @Autowired LineageRepository records;
  @Autowired com.policymesh.enforcement.DecisionRepository decisions;

  private DecisionRecord decision(String source, String destination, String from, String to, String outcome) {
    DecisionRecord d = new DecisionRecord();
    d.setSourceService(source);
    d.setDestinationService(destination);
    d.setSourceRegion(from);
    d.setDestinationRegion(to);
    d.setDataClass("PII");
    d.setDecision(outcome);
    d.setPolicyId("EU-PII-001");
    d.setReason("test reason");
    return decisions.save(d); // lineage records link a persisted decision's id
  }

  @Test
  void buildsAndVerifiesAValidChain() {
    var first = service.append(decision("a", "b", "EU", "EU", "ALLOW"));
    var second = service.append(decision("b", "c", "EU", "US", "DENY"));
    var third = service.append(decision("c", "d", "EU", "CN", "DENY"));

    assertThat(first.previousHash()).isNull();
    assertThat(second.previousHash()).isEqualTo(first.currentHash());
    assertThat(third.previousHash()).isEqualTo(second.currentHash());
    assertThat(first.currentHash()).hasSize(64);

    LineageDtos.Verification verification = service.verify();
    assertThat(verification.valid()).isTrue();
    assertThat(verification.recordsChecked()).isEqualTo(3);
    assertThat(verification.detail()).contains("valid");
  }

  @Test
  void detectsModifiedRecordContent() {
    service.append(decision("a", "b", "EU", "US", "DENY"));
    LineageRecord r = records.findAll().getFirst();
    r.setDecision("ALLOW"); // tamper: content no longer matches currentHash
    records.saveAndFlush(r);
    assertThat(service.verify().valid()).isFalse();
  }

  @Test
  void detectsBrokenPreviousHashLinkage() {
    service.append(decision("a", "b", "EU", "EU", "ALLOW"));
    service.append(decision("b", "c", "EU", "US", "DENY"));
    LineageRecord second = records.findAll().get(1);
    second.setPreviousHash("0000000000000000000000000000000000000000000000000000000000000000");
    records.saveAndFlush(second);
    var verification = service.verify();
    assertThat(verification.valid()).isFalse();
    assertThat(verification.brokenAt()).isEqualTo(second.getId());
  }

  @Test
  void detectsForgedCurrentHash() {
    service.append(decision("a", "b", "EU", "US", "DENY"));
    LineageRecord r = records.findAll().getFirst();
    r.setCurrentHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    records.saveAndFlush(r);
    assertThat(service.verify().valid()).isFalse();
  }

  @Test
  void everyRecordLinksItsDecision() {
    var record = service.append(decision("a", "b", "EU", "US", "DENY"));
    assertThat(record.decisionId()).isNotNull().isEqualTo(records.findAll().getFirst().getDecisionId());
    assertThat(record.decision()).isEqualTo("DENY");
    assertThat(record.reason()).isEqualTo("test reason");
  }
}
