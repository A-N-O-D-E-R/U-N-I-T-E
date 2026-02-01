package com.unite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionRequest {

    @NotBlank(message = "Workflow definition ID is required")
    private String workflowDefinitionId;

    private String caseId;

    private Map<String, Object> inputVariables;
}
