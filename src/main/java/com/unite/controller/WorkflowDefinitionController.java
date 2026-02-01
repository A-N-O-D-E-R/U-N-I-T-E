package com.unite.controller;

import com.unite.dto.WorkflowDefinitionRequest;
import com.unite.dto.WorkflowDefinitionResponse;
import com.unite.service.WorkflowDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-definitions")
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    @PostMapping
    public ResponseEntity<WorkflowDefinitionResponse> createWorkflowDefinition(
            @Valid @RequestBody WorkflowDefinitionRequest request) {
        log.info("POST /api/v1/workflow-definitions - Creating workflow definition: {}", request.getName());
        WorkflowDefinitionResponse response = workflowDefinitionService.createWorkflowDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDefinitionResponse> getWorkflowDefinition(@PathVariable String id) {
        log.info("GET /api/v1/workflow-definitions/{}", id);
        WorkflowDefinitionResponse response = workflowDefinitionService.getWorkflowDefinition(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDefinitionResponse>> getAllWorkflowDefinitions(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {
        log.info("GET /api/v1/workflow-definitions - active: {}, search: {}", active, search);

        List<WorkflowDefinitionResponse> responses;

        if (search != null && !search.isEmpty()) {
            responses = workflowDefinitionService.searchWorkflowDefinitions(search);
        } else if (active != null && active) {
            responses = workflowDefinitionService.getActiveWorkflowDefinitions();
        } else {
            responses = workflowDefinitionService.getAllWorkflowDefinitions();
        }

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDefinitionResponse> updateWorkflowDefinition(
            @PathVariable String id,
            @Valid @RequestBody WorkflowDefinitionRequest request) {
        log.info("PUT /api/v1/workflow-definitions/{}", id);
        WorkflowDefinitionResponse response = workflowDefinitionService.updateWorkflowDefinition(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflowDefinition(@PathVariable String id) {
        log.info("DELETE /api/v1/workflow-definitions/{}", id);
        workflowDefinitionService.deleteWorkflowDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<WorkflowDefinitionResponse> activateWorkflowDefinition(@PathVariable String id) {
        log.info("POST /api/v1/workflow-definitions/{}/activate", id);
        WorkflowDefinitionResponse response = workflowDefinitionService.activateWorkflowDefinition(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<WorkflowDefinitionResponse> deactivateWorkflowDefinition(@PathVariable String id) {
        log.info("POST /api/v1/workflow-definitions/{}/deactivate", id);
        WorkflowDefinitionResponse response = workflowDefinitionService.deactivateWorkflowDefinition(id);
        return ResponseEntity.ok(response);
    }
}
