package com.policymesh.servicegraph.controller;

import com.policymesh.servicegraph.dto.DataFlowEdgeRequest;
import com.policymesh.servicegraph.dto.DataFlowEdgeResponse;
import com.policymesh.servicegraph.service.DataFlowEdgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Data-flow edges are exposed under /api/v1/edges (a natural extension of
 * the /services resource group referenced in section 13 of the spec).
 */
@RestController
@RequestMapping("/api/v1/edges")
@RequiredArgsConstructor
public class DataFlowEdgeController {

    private final DataFlowEdgeService dataFlowEdgeService;

    @GetMapping
    public ResponseEntity<List<DataFlowEdgeResponse>> findAll() {
        return ResponseEntity.ok(dataFlowEdgeService.findAll());
    }

    @PostMapping
    public ResponseEntity<DataFlowEdgeResponse> create(@Valid @RequestBody DataFlowEdgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataFlowEdgeService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dataFlowEdgeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
