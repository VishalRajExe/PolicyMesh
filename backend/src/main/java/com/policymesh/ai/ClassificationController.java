package com.policymesh.ai;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/classify")
public class ClassificationController {
  private final ClassificationService service;

  public ClassificationController(ClassificationService service) { this.service = service; }

  @PostMapping
  public AiDtos.Response classify(@Valid @RequestBody AiDtos.Request r) {
    return service.classify(r);
  }

  /** Human approval is mandatory before a suggestion becomes enforcement-relevant. */
  @PostMapping("/{id}/approve")
  public AiDtos.Response approve(@PathVariable long id, Authentication auth) {
    return service.approve(id, auth == null ? null : auth.getName());
  }

  @PostMapping("/{id}/reject")
  public AiDtos.Response reject(@PathVariable long id, Authentication auth) {
    return service.reject(id, auth == null ? null : auth.getName());
  }
}
