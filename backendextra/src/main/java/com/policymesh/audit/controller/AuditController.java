package com.policymesh.audit.controller;

import com.policymesh.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> recent(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(auditService.recentActivity(limit));
    }
}
