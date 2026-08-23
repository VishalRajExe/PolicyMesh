package com.policymesh.ci;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ci")
public class CiController {
  private final CiService service;

  public CiController(CiService service) { this.service = service; }

  /** Always HTTP 200; the PASS/FAIL outcome is carried in the body (machine-readable) and humanReadable. */
  @PostMapping("/check")
  public CiDtos.Response check(@Valid @RequestBody CiDtos.Request r) {
    return service.run(r.branch(), r.commitHash());
  }

  @GetMapping("/branches")
  public java.util.List<String> branches() {
    return service.listBranches();
  }

  @GetMapping("/scans/{id}")
  public CiDtos.Response one(@PathVariable long id) { return service.one(id); }
}
