package com.unite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unite.dto.WorkflowDefinitionRequest;
import com.unite.dto.WorkflowDefinitionResponse;
import com.unite.exception.ResourceNotFoundException;
import com.unite.model.WorkflowDefinitionEntity;
import com.unite.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowDefinitionResponse createWorkflowDefinition(WorkflowDefinitionRequest request) {
        log.info("Creating workflow definition: {}", request.getName());

        if (repository.existsByNameAndVersion(request.getName(), request.getVersion())) {
            throw new IllegalArgumentException(
                    String.format("Workflow definition with name '%s' and version '%s' already exists",
                            request.getName(), request.getVersion()));
        }

        WorkflowDefinitionEntity entity = WorkflowDefinitionEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .version(request.getVersion())
                .definitionJson(convertToJsonString(request.getDefinitionJson()))
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(request.getCreatedBy())
                .tags(request.getTags())
                .build();

        WorkflowDefinitionEntity saved = repository.save(entity);
        log.info("Workflow definition created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse getWorkflowDefinition(String id) {
        log.debug("Fetching workflow definition: {}", id);
        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Definition", id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> getAllWorkflowDefinitions() {
        log.debug("Fetching all workflow definitions");
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> getActiveWorkflowDefinitions() {
        log.debug("Fetching active workflow definitions");
        return repository.findByActive(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> searchWorkflowDefinitions(String name) {
        log.debug("Searching workflow definitions by name: {}", name);
        return repository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowDefinitionResponse updateWorkflowDefinition(String id, WorkflowDefinitionRequest request) {
        log.info("Updating workflow definition: {}", id);

        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Definition", id));

        if (!entity.getName().equals(request.getName()) || !entity.getVersion().equals(request.getVersion())) {
            if (repository.existsByNameAndVersion(request.getName(), request.getVersion())) {
                throw new IllegalArgumentException(
                        String.format("Workflow definition with name '%s' and version '%s' already exists",
                                request.getName(), request.getVersion()));
            }
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setVersion(request.getVersion());
        entity.setDefinitionJson(convertToJsonString(request.getDefinitionJson()));
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity.setCreatedBy(request.getCreatedBy());
        entity.setTags(request.getTags());

        WorkflowDefinitionEntity updated = repository.save(entity);
        log.info("Workflow definition updated: {}", id);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteWorkflowDefinition(String id) {
        log.info("Deleting workflow definition: {}", id);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Workflow Definition", id);
        }

        repository.deleteById(id);
        log.info("Workflow definition deleted: {}", id);
    }

    @Transactional
    public WorkflowDefinitionResponse activateWorkflowDefinition(String id) {
        log.info("Activating workflow definition: {}", id);

        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Definition", id));

        entity.setActive(true);
        WorkflowDefinitionEntity updated = repository.save(entity);

        return mapToResponse(updated);
    }

    @Transactional
    public WorkflowDefinitionResponse deactivateWorkflowDefinition(String id) {
        log.info("Deactivating workflow definition: {}", id);

        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow Definition", id));

        entity.setActive(false);
        WorkflowDefinitionEntity updated = repository.save(entity);

        return mapToResponse(updated);
    }

    private WorkflowDefinitionResponse mapToResponse(WorkflowDefinitionEntity entity) {
        return WorkflowDefinitionResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .version(entity.getVersion())
                .definitionJson(parseJsonString(entity.getDefinitionJson()))
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .tags(entity.getTags())
                .build();
    }

    private String convertToJsonString(Object obj) {
        try {
            if (obj instanceof String) {
                return (String) obj;
            }
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
    }

    private JsonNode parseJsonString(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            return null;
        }
    }
}
