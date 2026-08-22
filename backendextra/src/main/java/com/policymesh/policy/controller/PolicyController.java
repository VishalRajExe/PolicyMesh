package com.policymesh.policy.controller;

import com.policymesh.policy.dto.PolicyRequest;
import com.policymesh.policy.dto.PolicyResponse;
import com.policymesh.policy.dto.PolicyYamlRequest;
import com.policymesh.policy.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<List<PolicyResponse>> findAll() {
        return ResponseEntity.ok(policyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @PostMapping("/yaml")
    public ResponseEntity<PolicyResponse> createFromYaml(@Valid @RequestBody PolicyYamlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createFromYaml(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyResponse> update(@PathVariable UUID id, @Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
