package com.unite.mapper;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.unite.model.WorkflowExecutionEntity;
import com.unite.model.WorkflowExecutionEntity.ExecutionStatus;

public class WorkflowExecutionMapper {
    public static WorkflowExecutionEntity map(WorkflowContext context){
        return WorkflowExecutionEntity.builder().id(context.getCaseId())
                .build();
    }

    private static ExecutionStatus mapStatus(String status) {
        if (status == null) {
            return ExecutionStatus.PENDING;
        }
        return switch (status.toUpperCase()) {
            case "RUNNING", "IN_PROGRESS" -> ExecutionStatus.RUNNING;
            case "COMPLETED", "SUCCESS" -> ExecutionStatus.COMPLETED;
            case "FAILED", "ERROR" -> ExecutionStatus.FAILED;
            case "CANCELLED", "CANCELED" -> ExecutionStatus.CANCELLED;
            default -> ExecutionStatus.PENDING;
        };
    }
}
