package com.unite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unite.dto.WorkflowEventMessage;
import com.unite.dto.WorkflowExecutionRequest;
import com.unite.dto.WorkflowExecutionResponse;
import com.unite.exception.ResourceNotFoundException;
import com.unite.exception.WorkflowExecutionException;
import com.unite.model.WorkflowDefinitionEntity;
import com.unite.model.WorkflowExecutionEntity;
import com.unite.repository.WorkflowDefinitionRepository;
import com.unite.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowExecutionService {

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    @Async
    public CompletableFuture<WorkflowExecutionResponse> executeWorkflowAsync(WorkflowExecutionRequest request) {
        log.info("Starting async workflow execution for definition: {}", request.getWorkflowDefinitionId());

        try {
            WorkflowExecutionResponse response = executeWorkflow(request);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error executing workflow: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public WorkflowExecutionResponse executeWorkflow(WorkflowExecutionRequest request) {
        log.info("Executing workflow for definition: {}", request.getWorkflowDefinitionId());

        WorkflowDefinitionEntity definition = definitionRepository.findById(request.getWorkflowDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Definition", request.getWorkflowDefinitionId()));

        if (!definition.getActive()) {
            throw new WorkflowExecutionException("Workflow definition is not active: " + definition.getName());
        }

        String caseId = request.getCaseId() != null ? request.getCaseId() : UUID.randomUUID().toString();

        WorkflowExecutionEntity execution = WorkflowExecutionEntity.builder()
                .workflowDefinitionId(definition.getId())
                .caseId(caseId)
                .status(WorkflowExecutionEntity.ExecutionStatus.PENDING)
                .inputVariables(convertToJsonString(request.getInputVariables()))
                .build();

        execution = executionRepository.save(execution);
        log.info("Workflow execution created with ID: {}", execution.getId());

        publishEvent(execution, WorkflowEventMessage.EventType.WORKFLOW_STARTED, "Workflow started");

        try {
            execution.setStatus(WorkflowExecutionEntity.ExecutionStatus.RUNNING);
            execution = executionRepository.save(execution);

            publishEvent(execution, WorkflowEventMessage.EventType.STATE_CHANGED, "Workflow running");

            Map<String, Object> outputVariables = simulateWorkflowExecution(
                    definition,
                    request.getInputVariables(),
                    execution.getId()
            );

            execution.setStatus(WorkflowExecutionEntity.ExecutionStatus.COMPLETED);
            execution.setOutputVariables(convertToJsonString(outputVariables));
            execution.setCompletedAt(LocalDateTime.now());
            execution = executionRepository.save(execution);

            publishEvent(execution, WorkflowEventMessage.EventType.WORKFLOW_COMPLETED, "Workflow completed successfully");

            log.info("Workflow execution completed: {}", execution.getId());

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage(), e);

            execution.setStatus(WorkflowExecutionEntity.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution = executionRepository.save(execution);

            publishEvent(execution, WorkflowEventMessage.EventType.WORKFLOW_FAILED, "Workflow failed: " + e.getMessage());

            throw new WorkflowExecutionException("Workflow execution failed", e);
        }

        return mapToResponse(execution);
    }

    private Map<String, Object> simulateWorkflowExecution(
            WorkflowDefinitionEntity definition,
            Map<String, Object> inputVariables,
            String executionId) throws InterruptedException {

        log.info("Simulating workflow execution for: {}", definition.getName());

        publishEvent(executionId, definition.getId(), WorkflowEventMessage.EventType.STEP_STARTED,
                "Processing workflow steps", null);

        Thread.sleep(2000);

        publishEvent(executionId, definition.getId(), WorkflowEventMessage.EventType.STEP_COMPLETED,
                "All steps completed", null);

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("status", "success");
        outputs.put("processedAt", LocalDateTime.now().toString());
        if (inputVariables != null) {
            outputs.putAll(inputVariables);
        }

        return outputs;
    }

    @Transactional(readOnly = true)
    public WorkflowExecutionResponse getExecution(String executionId) {
        log.debug("Fetching execution: {}", executionId);
        WorkflowExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Execution", executionId));
        return mapToResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> getAllExecutions() {
        log.debug("Fetching all executions");
        return executionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> getExecutionsByDefinition(String definitionId) {
        log.debug("Fetching executions for definition: {}", definitionId);
        return executionRepository.findByWorkflowDefinitionId(definitionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> getExecutionsByStatus(WorkflowExecutionEntity.ExecutionStatus status) {
        log.debug("Fetching executions by status: {}", status);
        return executionRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowExecutionResponse cancelExecution(String executionId) {
        log.info("Cancelling execution: {}", executionId);

        WorkflowExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Execution", executionId));

        if (execution.getStatus() == WorkflowExecutionEntity.ExecutionStatus.COMPLETED ||
                execution.getStatus() == WorkflowExecutionEntity.ExecutionStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel execution in " + execution.getStatus() + " state");
        }

        execution.setStatus(WorkflowExecutionEntity.ExecutionStatus.CANCELLED);
        execution.setCompletedAt(LocalDateTime.now());
        execution = executionRepository.save(execution);

        publishEvent(execution, WorkflowEventMessage.EventType.STATE_CHANGED, "Workflow cancelled");

        return mapToResponse(execution);
    }

    private void publishEvent(WorkflowExecutionEntity execution, WorkflowEventMessage.EventType eventType, String message) {
        publishEvent(execution.getId(), execution.getWorkflowDefinitionId(), eventType, message, null);
    }

    private void publishEvent(String executionId, String definitionId, WorkflowEventMessage.EventType eventType,
                               String message, Map<String, Object> data) {
        try {
            WorkflowEventMessage event = WorkflowEventMessage.builder()
                    .executionId(executionId)
                    .workflowDefinitionId(definitionId)
                    .eventType(eventType)
                    .message(message)
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Error publishing event: {}", e.getMessage(), e);
        }
    }

    private WorkflowExecutionResponse mapToResponse(WorkflowExecutionEntity entity) {
        return WorkflowExecutionResponse.builder()
                .id(entity.getId())
                .workflowDefinitionId(entity.getWorkflowDefinitionId())
                .caseId(entity.getCaseId())
                .status(entity.getStatus())
                .inputVariables(parseJsonString(entity.getInputVariables()))
                .outputVariables(parseJsonString(entity.getOutputVariables()))
                .errorMessage(entity.getErrorMessage())
                .startedAt(entity.getStartedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    private String convertToJsonString(Object obj) {
        try {
            if (obj == null) {
                return null;
            }
            if (obj instanceof String) {
                return (String) obj;
            }
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to JSON: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseJsonString(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
