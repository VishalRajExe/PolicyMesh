package com.policymesh.ci;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ci")
public class CiController {
  private final CiService service;

  public CiController(CiService service) { this.service = service; }

  /**
   * Evaluates proposed changes on target branch & commit against active PolicyMesh AST rules.
   * If commit/branch is invalid, returns 422 with structured problem details.
   * If valid, returns 200 with PASSED or BLOCKED status and comprehensive breakdown.
   */
  @PostMapping("/check")
  public CiDtos.Response check(@Valid @RequestBody CiDtos.Request r) {
    return service.run(r.branch(), r.commitHash());
  }

  @GetMapping("/branches")
  public List<String> branches() {
    return service.listBranches();
  }

  @GetMapping("/scans")
  public Page<CiDtos.Response> listScans(@PageableDefault(size = 10) Pageable pageable) {
    return service.listScans(pageable);
  }

  @GetMapping("/scans/{id}")
  public CiDtos.Response one(@PathVariable long id) {
    return service.one(id);
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/scans")
  public void clearAll() {
    service.clearAllScans();
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/scans/{id}")
  public void deleteOne(@PathVariable long id) {
    service.deleteScan(id);
  }
}
