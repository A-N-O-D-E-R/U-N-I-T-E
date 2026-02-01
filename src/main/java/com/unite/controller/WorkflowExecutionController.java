package com.unite.controller;

import com.unite.dto.WorkflowExecutionRequest;
import com.unite.dto.WorkflowExecutionResponse;
import com.unite.model.WorkflowExecutionEntity;
import com.unite.service.WorkflowExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/workflow-executions")
@RequiredArgsConstructor
@Slf4j
public class WorkflowExecutionController {

    private final WorkflowExecutionService workflowExecutionService;

    @PostMapping
    public ResponseEntity<WorkflowExecutionResponse> executeWorkflow(
            @Valid @RequestBody WorkflowExecutionRequest request,
            @RequestParam(defaultValue = "false") boolean async) {
        log.info("POST /api/v1/workflow-executions - Executing workflow: {} (async: {})",
                request.getWorkflowDefinitionId(), async);

        if (async) {
            workflowExecutionService.executeWorkflowAsync(request);
            WorkflowExecutionResponse response = WorkflowExecutionResponse.builder()
                    .workflowDefinitionId(request.getWorkflowDefinitionId())
                    .caseId(request.getCaseId())
                    .status(WorkflowExecutionEntity.ExecutionStatus.PENDING)
                    .build();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } else {
            WorkflowExecutionResponse response = workflowExecutionService.executeWorkflow(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<List<WorkflowExecutionResponse>> executeWorkflowsInParallel(
            @Valid @RequestBody List<WorkflowExecutionRequest> requests) {
        log.info("POST /api/v1/workflow-executions/batch - Executing {} workflows in parallel", requests.size());

        List<CompletableFuture<WorkflowExecutionResponse>> futures = new ArrayList<>();

        for (WorkflowExecutionRequest request : requests) {
            CompletableFuture<WorkflowExecutionResponse> future =
                    workflowExecutionService.executeWorkflowAsync(request);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<WorkflowExecutionResponse> responses = new ArrayList<>();
        for (CompletableFuture<WorkflowExecutionResponse> future : futures) {
            try {
                responses.add(future.get());
            } catch (Exception e) {
                log.error("Error executing workflow in batch: {}", e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowExecutionResponse> getExecution(@PathVariable String id) {
        log.info("GET /api/v1/workflow-executions/{}", id);
        WorkflowExecutionResponse response = workflowExecutionService.getExecution(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowExecutionResponse>> getAllExecutions(
            @RequestParam(required = false) String definitionId,
            @RequestParam(required = false) WorkflowExecutionEntity.ExecutionStatus status) {
        log.info("GET /api/v1/workflow-executions - definitionId: {}, status: {}", definitionId, status);

        List<WorkflowExecutionResponse> responses;

        if (definitionId != null) {
            responses = workflowExecutionService.getExecutionsByDefinition(definitionId);
        } else if (status != null) {
            responses = workflowExecutionService.getExecutionsByStatus(status);
        } else {
            responses = workflowExecutionService.getAllExecutions();
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<WorkflowExecutionResponse> cancelExecution(@PathVariable String id) {
        log.info("POST /api/v1/workflow-executions/{}/cancel", id);
        WorkflowExecutionResponse response = workflowExecutionService.cancelExecution(id);
        return ResponseEntity.ok(response);
    }
}
