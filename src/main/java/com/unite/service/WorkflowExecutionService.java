package com.unite.service;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilder;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unite.dto.WorkflowEventMessage;
import com.unite.dto.WorkflowExecutionRequest;
import com.unite.dto.WorkflowExecutionResponse;
import com.unite.exception.ResourceNotFoundException;
import com.unite.exception.WorkflowExecutionException;
import com.unite.mapper.WorkflowExecutionMapper;
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
    private final FluentWorkflowBuilderFactory workflowFactory;
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
        FluentWorkflowBuilder builder = workflowFactory.builder(caseId);
        
        WorkflowDefinition workflowDefinition = mapToWorkflowDefinition(definition.getDefinitionJson());
        WorkflowVariables workflowVariables = mapToWorkflowVariables(request.getInputVariables());
        WorkflowContext context = builder.start(workflowDefinition, workflowVariables);

        WorkflowExecutionEntity entity = executionRepository.save(WorkflowExecutionMapper.map(context));
        return mapToResponse(entity) ;
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

    private WorkflowDefinition mapToWorkflowDefinition(String definitionJson) {
        try {
            return objectMapper.readValue(definitionJson, WorkflowDefinition.class);
        } catch (Exception e) {
            throw new WorkflowExecutionException("Failed to parse workflow definition JSON: " + e.getMessage(), e);
        }
    }

    private WorkflowVariables mapToWorkflowVariables(Map<String, Object> inputVariables) {
        if (inputVariables == null || inputVariables.isEmpty()) {
            return new WorkflowVariables();
        }
        return objectMapper.convertValue(inputVariables, WorkflowVariables.class);
    }
}
