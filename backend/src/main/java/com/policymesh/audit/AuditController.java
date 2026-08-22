package com.policymesh.audit;

import com.policymesh.enforcement.DecisionDtos;
import com.policymesh.enforcement.DecisionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only audit view over recent runtime decisions (the write-ahead audit trail is the lineage chain). */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
  private final DecisionRepository decisions;

  public AuditController(DecisionRepository decisions) { this.decisions = decisions; }

  @GetMapping("/decisions")
  public java.util.List<DecisionDtos.Response> recentDecisions() {
    return DecisionDtos.from(decisions.findTop100ByOrderByCreatedAtDesc());
  }
}
