package com.policymesh.enforcement.controller;

import com.policymesh.enforcement.dto.EnforcementCheckRequest;
import com.policymesh.enforcement.dto.EnforcementCheckResponse;
import com.policymesh.enforcement.service.EnforcementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enforce")
@RequiredArgsConstructor
public class EnforcementController {

    private final EnforcementService enforcementService;

    @PostMapping("/check")
    public ResponseEntity<EnforcementCheckResponse> check(@Valid @RequestBody EnforcementCheckRequest request) {
        return ResponseEntity.ok(enforcementService.check(request));
    }
}
