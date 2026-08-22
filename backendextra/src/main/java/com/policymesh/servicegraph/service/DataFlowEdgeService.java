package com.policymesh.servicegraph.service;

import com.policymesh.common.exception.ResourceNotFoundException;
import com.policymesh.servicegraph.dto.DataFlowEdgeRequest;
import com.policymesh.servicegraph.dto.DataFlowEdgeResponse;
import com.policymesh.servicegraph.entity.DataFlowEdge;
import com.policymesh.servicegraph.entity.ServiceNode;
import com.policymesh.servicegraph.repository.DataFlowEdgeRepository;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataFlowEdgeService {

    private final DataFlowEdgeRepository dataFlowEdgeRepository;
    private final ServiceNodeRepository serviceNodeRepository;

    @Transactional(readOnly = true)
    public List<DataFlowEdgeResponse> findAll() {
        return dataFlowEdgeRepository.findAll().stream().map(DataFlowEdgeResponse::from).toList();
    }

    @Transactional
    public DataFlowEdgeResponse create(DataFlowEdgeRequest request) {
        ServiceNode source = serviceNodeRepository.findById(request.sourceServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Source service not found: " + request.sourceServiceId()));
        ServiceNode destination = serviceNodeRepository.findById(request.destinationServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination service not found: " + request.destinationServiceId()));

        DataFlowEdge edge = DataFlowEdge.builder()
                .source(source)
                .destination(destination)
                .dataClasses(String.join(",", request.dataClasses().stream().map(String::toUpperCase).toList()))
                .build();

        return DataFlowEdgeResponse.from(dataFlowEdgeRepository.save(edge));
    }

    @Transactional
    public void delete(UUID id) {
        DataFlowEdge edge = dataFlowEdgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data flow edge not found: " + id));
        dataFlowEdgeRepository.delete(edge);
    }
}
