package com.unite.dto;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.unite.model.WorkflowExecutionEntity.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionResponse {

    private String id;
    private String workflowDefinitionId;
    private String caseId;
    private ExecutionStatus status;
    private Map<String, Object> inputVariables;
    private Map<String, Object> outputVariables;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

}
