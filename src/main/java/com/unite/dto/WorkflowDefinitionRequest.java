package com.unite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionRequest {

    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;

    @NotBlank(message = "Version is required")
    private String version;

    @NotNull(message = "Definition JSON is required")
    private Object definitionJson;

    private Boolean active;

    private String createdBy;

    private String tags;
}
