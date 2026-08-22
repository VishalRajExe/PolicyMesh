package com.policymesh.enforcement;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/enforce", "/api/v1/compliance"})
public class EnforcementController {
  private final EnforcementService service;

  public EnforcementController(EnforcementService service) { this.service = service; }

  /** A DENY outcome is a valid business result and returns HTTP 200. */
  @PostMapping("/check")
  public EnforcementDtos.Response check(@Valid @RequestBody EnforcementDtos.Request r) {
    return service.check(r);
  }
}
