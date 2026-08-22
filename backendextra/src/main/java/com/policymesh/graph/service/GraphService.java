package com.policymesh.graph.service;

import com.policymesh.graph.analyzer.GraphAnalyzer;
import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.validator.GraphValidator;
import com.policymesh.servicegraph.dto.DataFlowEdgeResponse;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;
import com.policymesh.servicegraph.repository.DataFlowEdgeRepository;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final ServiceNodeRepository serviceNodeRepository;
    private final DataFlowEdgeRepository dataFlowEdgeRepository;
    private final GraphValidator graphValidator;
    private final GraphAnalyzer graphAnalyzer;

    @Transactional(readOnly = true)
    public Map<String, Object> getGraph() {
        List<ServiceNodeResponse> nodes = serviceNodeRepository.findAll().stream()
                .map(ServiceNodeResponse::from).toList();
        List<DataFlowEdgeResponse> edges = dataFlowEdgeRepository.findAll().stream()
                .map(DataFlowEdgeResponse::from).toList();
        return Map.of("nodes", nodes, "edges", edges);
    }

    @Transactional(readOnly = true)
    public GraphCheckResult validate() {
        graphValidator.validate(dataFlowEdgeRepository.findAll());
        return graphAnalyzer.analyze();
    }
}
