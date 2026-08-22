package com.policymesh.graph.validator;

import com.policymesh.common.exception.InvalidPolicyException;
import com.policymesh.servicegraph.entity.DataFlowEdge;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Structural sanity checks on the service graph, run before policy
 * evaluation (e.g. rejecting self-referential edges with no data classes).
 */
@Component
public class GraphValidator {

    public void validate(List<DataFlowEdge> edges) {
        for (DataFlowEdge edge : edges) {
            if (edge.getSource() == null || edge.getDestination() == null) {
                throw new InvalidPolicyException("Data flow edge " + edge.getId() + " is missing a source or destination service");
            }
            if (edge.dataClassList().isEmpty()) {
                throw new InvalidPolicyException("Data flow edge " + edge.getId() + " has no data classification tags");
            }
        }
    }
}
