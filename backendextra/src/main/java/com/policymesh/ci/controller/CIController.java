package com.policymesh.ci.controller;

import com.policymesh.ci.dto.CIScanRequest;
import com.policymesh.ci.dto.CIScanResponse;
import com.policymesh.ci.service.CIScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ci")
@RequiredArgsConstructor
public class CIController {

    private final CIScanService ciScanService;

    @PostMapping("/check")
    public ResponseEntity<CIScanResponse> check(@RequestBody(required = false) CIScanRequest request) {
        return ResponseEntity.ok(ciScanService.runScan(request));
    }

    @GetMapping("/scans/{id}")
    public ResponseEntity<CIScanResponse> getScan(@PathVariable UUID id) {
        return ResponseEntity.ok(ciScanService.findById(id));
    }
}
