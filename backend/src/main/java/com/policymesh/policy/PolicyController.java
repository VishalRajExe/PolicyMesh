package com.policymesh.policy;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {
  private final PolicyService service;

  public PolicyController(PolicyService service) { this.service = service; }

  @GetMapping
  public List<PolicyDtos.Response> all() { return service.all(); }

  @GetMapping("/{id}")
  public PolicyDtos.Response one(@PathVariable long id) { return service.one(id); }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PolicyDtos.Response create(@Valid @RequestBody PolicyDtos.Request r) { return service.create(r); }

  @PostMapping(value = "/yaml", consumes = {"application/json", "text/plain", "application/x-yaml", "*/*"})
  @ResponseStatus(HttpStatus.CREATED)
  public Object createFromYaml(@RequestBody String body) {
    String yaml = body;
    if (body != null && body.trim().startsWith("{")) {
      try {
        var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        if (node.has("yaml") && node.get("yaml").isTextual()) {
          yaml = node.get("yaml").asText();
        }
      } catch (Exception ignored) {}
    }
    var list = service.importYaml(yaml);
    return list.size() == 1 ? list.get(0) : list;
  }

  @PutMapping("/{id}")
  public PolicyDtos.Response update(@PathVariable long id, @Valid @RequestBody PolicyDtos.Request r) {
    return service.update(id, r);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) { service.delete(id); }
}
