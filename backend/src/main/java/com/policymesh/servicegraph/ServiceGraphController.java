package com.policymesh.servicegraph;

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
@RequestMapping("/api/v1")
public class ServiceGraphController {
  private final ServiceGraphService s;

  public ServiceGraphController(ServiceGraphService s) { this.s = s; }

  @GetMapping("/services")
  public List<GraphDtos.ServiceResponse> services() { return s.allServices(); }

  @GetMapping("/services/{id}")
  public GraphDtos.ServiceResponse service(@PathVariable long id) { return s.service(id); }

  @PostMapping("/services")
  @ResponseStatus(HttpStatus.CREATED)
  public GraphDtos.ServiceResponse createService(@Valid @RequestBody GraphDtos.ServiceRequest r) {
    return s.createService(r);
  }

  @PutMapping("/services/{id}")
  public GraphDtos.ServiceResponse updateService(@PathVariable long id, @Valid @RequestBody GraphDtos.ServiceRequest r) {
    return s.updateService(id, r);
  }

  @DeleteMapping("/services/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteService(@PathVariable long id) { s.deleteService(id); }

  @GetMapping("/edges")
  public List<GraphDtos.EdgeResponse> edges() { return s.allEdges(); }

  @GetMapping("/edges/{id}")
  public GraphDtos.EdgeResponse edge(@PathVariable long id) { return s.edge(id); }

  @PostMapping("/edges")
  @ResponseStatus(HttpStatus.CREATED)
  public GraphDtos.EdgeResponse createEdge(@Valid @RequestBody GraphDtos.EdgeRequest r) {
    return s.createEdge(r);
  }

  @PutMapping("/edges/{id}")
  public GraphDtos.EdgeResponse updateEdge(@PathVariable long id, @Valid @RequestBody GraphDtos.EdgeRequest r) {
    return s.updateEdge(id, r);
  }

  @DeleteMapping("/edges/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteEdge(@PathVariable long id) { s.deleteEdge(id); }
}
