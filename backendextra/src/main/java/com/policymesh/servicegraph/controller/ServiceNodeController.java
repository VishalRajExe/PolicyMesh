package com.policymesh.servicegraph.controller;

import com.policymesh.servicegraph.dto.ServiceNodeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;
import com.policymesh.servicegraph.service.ServiceNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceNodeController {

    private final ServiceNodeService serviceNodeService;

    @GetMapping
    public ResponseEntity<List<ServiceNodeResponse>> findAll() {
        return ResponseEntity.ok(serviceNodeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceNodeResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceNodeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceNodeResponse> create(@Valid @RequestBody ServiceNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceNodeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceNodeResponse> update(@PathVariable UUID id, @Valid @RequestBody ServiceNodeRequest request) {
        return ResponseEntity.ok(serviceNodeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceNodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
