package com.policymesh.lineage.controller;

import com.policymesh.common.exception.ResourceNotFoundException;
import com.policymesh.lineage.dto.LineageRecordResponse;
import com.policymesh.lineage.dto.LineageVerificationResponse;
import com.policymesh.lineage.service.LineageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    @GetMapping
    public ResponseEntity<List<LineageRecordResponse>> findAll() {
        return ResponseEntity.ok(lineageService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LineageRecordResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(lineageService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lineage record not found: " + id)));
    }

    @GetMapping("/verify")
    public ResponseEntity<LineageVerificationResponse> verify() {
        return ResponseEntity.ok(lineageService.verifyChain());
    }
}
