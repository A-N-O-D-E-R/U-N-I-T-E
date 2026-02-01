package com.unite.dto;

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
public class WorkflowEventMessage {

    private String executionId;
    private String caseId;
    private String workflowDefinitionId;
    private EventType eventType;
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;

    public enum EventType {
        WORKFLOW_STARTED,
        WORKFLOW_COMPLETED,
        WORKFLOW_FAILED,
        STEP_STARTED,
        STEP_COMPLETED,
        STEP_FAILED,
        STATE_CHANGED
    }
}
