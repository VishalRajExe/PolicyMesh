package com.policymesh.graph;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {
  private final GraphAnalyzer analyzer;

  public GraphController(GraphAnalyzer analyzer) { this.analyzer = analyzer; }

  @GetMapping
  public GraphModels.View graph() { return analyzer.graph(); }

  /** Compliance violations are a valid business outcome and always return HTTP 200. */
  @PostMapping({"/validate", "/re-evaluate"})
  public GraphModels.CheckResult validate() { return analyzer.validate(); }
}
