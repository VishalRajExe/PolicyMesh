package com.policymesh.lineage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lineage")
public class LineageController {
  private final LineageService service;

  public LineageController(LineageService service) { this.service = service; }

  @GetMapping
  public List<LineageDtos.Response> all(@RequestParam(required = false) String decision,
                                        @RequestParam(required = false) String service) {
    return this.service.all(decision, service);
  }

  @GetMapping("/verify")
  public LineageDtos.Verification verify() { return service.verify(); }

  @GetMapping("/{id}")
  public LineageDtos.Response one(@PathVariable long id) { return service.one(id); }
}
