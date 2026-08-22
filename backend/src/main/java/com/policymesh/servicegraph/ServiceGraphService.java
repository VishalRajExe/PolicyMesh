package com.policymesh.servicegraph;

import com.policymesh.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.TreeSet;

@Service
@Transactional
public class ServiceGraphService {
  private final ServiceNodeRepository services;
  private final DataFlowEdgeRepository edges;

  public ServiceGraphService(ServiceNodeRepository services, DataFlowEdgeRepository edges) {
    this.services = services;
    this.edges = edges;
  }

  public List<GraphDtos.ServiceResponse> allServices() {
    return services.findAll().stream().map(GraphDtos::service).toList();
  }

  public GraphDtos.ServiceResponse service(long id) {
    return GraphDtos.service(serviceEntity(id));
  }

  public GraphDtos.ServiceResponse createService(GraphDtos.ServiceRequest r) {
    if (services.findByNameIgnoreCase(r.name().trim()).isPresent()) {
      throw ApiException.conflict("Service name already exists");
    }
    ServiceNode s = new ServiceNode();
    apply(s, r);
    return GraphDtos.service(services.save(s));
  }

  public GraphDtos.ServiceResponse updateService(long id, GraphDtos.ServiceRequest r) {
    ServiceNode s = serviceEntity(id);
    services.findByNameIgnoreCase(r.name().trim())
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> { throw ApiException.conflict("Service name already exists"); });
    apply(s, r);
    return GraphDtos.service(services.save(s));
  }

  public void deleteService(long id) {
    serviceEntity(id);
    edges.deleteAll(edges.findBySourceServiceIdOrDestinationServiceId(id, id));
    services.deleteById(id);
  }

  public List<GraphDtos.EdgeResponse> allEdges() {
    return edges.findAll().stream().map(GraphDtos::edge).toList();
  }

  public GraphDtos.EdgeResponse edge(long id) {
    return GraphDtos.edge(edgeEntity(id));
  }

  public GraphDtos.EdgeResponse createEdge(GraphDtos.EdgeRequest r) {
    validateEdge(r, null);
    if (edges.findBySourceServiceIdAndDestinationServiceId(r.sourceServiceId(), r.destinationServiceId()).isPresent()) {
      throw ApiException.conflict("Data flow edge already exists between these services");
    }
    DataFlowEdge e = new DataFlowEdge();
    apply(e, r);
    return GraphDtos.edge(edges.save(e));
  }

  public GraphDtos.EdgeResponse updateEdge(long id, GraphDtos.EdgeRequest r) {
    DataFlowEdge e = edgeEntity(id);
    validateEdge(r, id);
    edges.findBySourceServiceIdAndDestinationServiceId(r.sourceServiceId(), r.destinationServiceId())
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> { throw ApiException.conflict("Data flow edge already exists between these services"); });
    apply(e, r);
    return GraphDtos.edge(edges.save(e));
  }

  public void deleteEdge(long id) {
    edges.delete(edgeEntity(id));
  }

  public ServiceNode serviceEntity(long id) {
    return services.findById(id).orElseThrow(() -> ApiException.notFound("Service not found"));
  }

  private DataFlowEdge edgeEntity(long id) {
    return edges.findById(id).orElseThrow(() -> ApiException.notFound("Data flow edge not found"));
  }

  private void validateEdge(GraphDtos.EdgeRequest r, Long ignoreEdgeId) {
    if (r.sourceServiceId().equals(r.destinationServiceId())) {
      throw ApiException.unprocessable("Source and destination services must differ");
    }
    if (services.findById(r.sourceServiceId()).isEmpty()) {
      throw ApiException.unprocessable("Source service " + r.sourceServiceId() + " does not exist");
    }
    if (services.findById(r.destinationServiceId()).isEmpty()) {
      throw ApiException.unprocessable("Destination service " + r.destinationServiceId() + " does not exist");
    }
  }

  private void apply(ServiceNode s, GraphDtos.ServiceRequest r) {
    s.setName(r.name().trim());
    s.setRegion(r.region());
    s.setMeshZone(r.meshZone());
    s.setEnvironment(r.environment().trim());
    s.setDescription(r.description());
  }

  private void apply(DataFlowEdge e, GraphDtos.EdgeRequest r) {
    e.setSourceServiceId(r.sourceServiceId());
    e.setDestinationServiceId(r.destinationServiceId());
    TreeSet<String> classes = GraphDtos.canonicalClasses(r.dataClasses());
    if (classes.isEmpty()) throw ApiException.unprocessable("dataClasses must contain at least one entry");
    e.setDataClasses(classes);
  }
}
