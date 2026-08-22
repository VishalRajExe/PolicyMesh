package com.policymesh.ai.controller;

import com.policymesh.ai.dto.ClassifyRequest;
import com.policymesh.ai.dto.ClassifyResponse;
import com.policymesh.ai.dto.FieldClassification;
import com.policymesh.ai.service.ClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIClassificationController {

    private final ClassificationService classificationService;

    @PostMapping("/classify")
    public ResponseEntity<ClassifyResponse> classify(@Valid @RequestBody ClassifyRequest request) {
        return ResponseEntity.ok(classificationService.classify(request));
    }

    @PostMapping("/classify/{id}/approve")
    public ResponseEntity<FieldClassification> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(classificationService.approve(id));
    }

    @PostMapping("/classify/{id}/reject")
    public ResponseEntity<FieldClassification> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(classificationService.reject(id));
    }
}
