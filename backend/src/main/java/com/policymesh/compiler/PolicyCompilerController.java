package com.policymesh.compiler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Validates and compiles the YAML DSL without persisting anything. */
@RestController
@RequestMapping("/api/v1/compiler")
public class PolicyCompilerController {
  private final PolicyCompiler compiler;

  public PolicyCompilerController(PolicyCompiler compiler) { this.compiler = compiler; }

  public record Request(@NotBlank String yaml) {}

  @PostMapping("/compile")
  public CompiledPolicy compile(@Valid @RequestBody Request r) {
    return compiler.compile(r.yaml());
  }
}
