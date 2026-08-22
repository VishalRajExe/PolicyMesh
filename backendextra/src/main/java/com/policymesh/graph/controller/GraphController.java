package com.policymesh.graph.controller;

import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getGraph() {
        return ResponseEntity.ok(graphService.getGraph());
    }

    @PostMapping("/validate")
    public ResponseEntity<GraphCheckResult> validate() {
        return ResponseEntity.ok(graphService.validate());
    }
}
