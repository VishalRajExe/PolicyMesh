package com.policymesh.servicegraph.service.impl;

import com.policymesh.common.response.ApiResponse;
import com.policymesh.servicegraph.dto.DataFlowEdgeRequest;
import com.policymesh.servicegraph.dto.DataFlowEdgeResponse;
import com.policymesh.servicegraph.entity.DataFlowEdge;
import com.policymesh.servicegraph.repository.DataFlowEdgeRepository;
import com.policymesh.servicegraph.service.DataFlowEdgeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the DataFlowEdgeService interface.
 * Handles CRUD operations for data flow edges in the service mesh.
 */
@Service
@RequiredArgsConstructor
public class DataFlowEdgeServiceImpl implements DataFlowEdgeService {

    private final DataFlowEdgeRepository dataFlowEdgeRepository;
    private final ModelMapper modelMapper;

    @Override
    public DataFlowEdgeResponse createDataFlowEdge(DataFlowEdgeRequest request) {
        // Validate that source and destination services are different
        if (request.getSourceServiceId().equals(request.getDestinationServiceId())) {
            throw new RuntimeException("Source and destination services must be different");
        }

        DataFlowEdge dataFlowEdge = modelMapper.map(request, DataFlowEdge.class);
        DataFlowEdge savedDataFlowEdge = dataFlowEdgeRepository.save(dataFlowEdge);
        return modelMapper.map(savedDataFlowEdge, DataFlowEdgeResponse.class);
    }

    @Override
    public DataFlowEdgeResponse getDataFlowEdgeById(Long id) {
        DataFlowEdge dataFlowEdge = dataFlowEdgeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Data flow edge not found with id: " + id));
        return modelMapper.map(dataFlowEdge, DataFlowEdgeResponse.class);
    }

    @Override
    public List<DataFlowEdgeResponse> getAllDataFlowEdges() {
        List<DataFlowEdge> dataFlowEdges = dataFlowEdgeRepository.findAll();
        return dataFlowEdges.stream()
                .map(dataFlowEdge -> modelMapper.map(dataFlowEdge, DataFlowEdgeResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public DataFlowEdgeResponse updateDataFlowEdge(Long id, DataFlowEdgeRequest request) {
        DataFlowEdge existingDataFlowEdge = dataFlowEdgeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Data flow edge not found with id: " + id));

        // Validate that source and destination services are different
        if (request.getSourceServiceId().equals(request.getDestinationServiceId())) {
            throw new RuntimeException("Source and destination services must be different");
        }

        modelMapper.map(request, existingDataFlowEdge);
        DataFlowEdge updatedDataFlowEdge = dataFlowEdgeRepository.save(existingDataFlowEdge);
        return modelMapper.map(updatedDataFlowEdge, DataFlowEdgeResponse.class);
    }

    @Override
    public void deleteDataFlowEdge(Long id) {
        if (!dataFlowEdgeRepository.existsById(id)) {
            throw new RuntimeException("Data flow edge not found with id: " + id);
        }
        dataFlowEdgeRepository.deleteById(id);
    }
}