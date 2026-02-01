package com.unite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unite.dto.WorkflowDefinitionRequest;
import com.unite.dto.WorkflowDefinitionResponse;
import com.unite.exception.ResourceNotFoundException;
import com.unite.model.WorkflowDefinitionEntity;
import com.unite.repository.WorkflowDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionServiceTest {

    @Mock
    private WorkflowDefinitionRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkflowDefinitionService service;

    private WorkflowDefinitionRequest request;
    private WorkflowDefinitionEntity entity;

    @BeforeEach
    void setUp() {
        Map<String, Object> definitionJson = new HashMap<>();
        definitionJson.put("steps", Arrays.asList(
                Map.of("id", "step1", "type", "test")
        ));

        request = WorkflowDefinitionRequest.builder()
                .name("test-workflow")
                .description("Test workflow")
                .version("1.0.0")
                .definitionJson(definitionJson)
                .active(true)
                .createdBy("test-user")
                .build();

        entity = WorkflowDefinitionEntity.builder()
                .id("test-id")
                .name("test-workflow")
                .description("Test workflow")
                .version("1.0.0")
                .definitionJson("{\"steps\":[]}")
                .active(true)
                .createdBy("test-user")
                .build();
    }

    @Test
    void createWorkflowDefinition_Success() throws Exception {
        when(repository.existsByNameAndVersion(anyString(), anyString())).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"steps\":[]}");
        when(repository.save(any(WorkflowDefinitionEntity.class))).thenReturn(entity);
        when(objectMapper.readTree(anyString())).thenReturn(objectMapper.createObjectNode());

        WorkflowDefinitionResponse response = service.createWorkflowDefinition(request);

        assertNotNull(response);
        assertEquals("test-id", response.getId());
        assertEquals("test-workflow", response.getName());
        verify(repository, times(1)).save(any(WorkflowDefinitionEntity.class));
    }

    @Test
    void createWorkflowDefinition_DuplicateName() {
        when(repository.existsByNameAndVersion(anyString(), anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            service.createWorkflowDefinition(request);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void getWorkflowDefinition_Success() throws Exception {
        when(repository.findById(anyString())).thenReturn(Optional.of(entity));
        when(objectMapper.readTree(anyString())).thenReturn(objectMapper.createObjectNode());

        WorkflowDefinitionResponse response = service.getWorkflowDefinition("test-id");

        assertNotNull(response);
        assertEquals("test-id", response.getId());
        verify(repository, times(1)).findById("test-id");
    }

    @Test
    void getWorkflowDefinition_NotFound() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getWorkflowDefinition("non-existent-id");
        });
    }

    @Test
    void getAllWorkflowDefinitions() throws Exception {
        when(repository.findAll()).thenReturn(Arrays.asList(entity));
        when(objectMapper.readTree(anyString())).thenReturn(objectMapper.createObjectNode());

        List<WorkflowDefinitionResponse> responses = service.getAllWorkflowDefinitions();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void deleteWorkflowDefinition_Success() {
        when(repository.existsById(anyString())).thenReturn(true);

        service.deleteWorkflowDefinition("test-id");

        verify(repository, times(1)).deleteById("test-id");
    }

    @Test
    void deleteWorkflowDefinition_NotFound() {
        when(repository.existsById(anyString())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteWorkflowDefinition("non-existent-id");
        });

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void activateWorkflowDefinition() throws Exception {
        entity.setActive(false);
        when(repository.findById(anyString())).thenReturn(Optional.of(entity));
        when(repository.save(any(WorkflowDefinitionEntity.class))).thenReturn(entity);
        when(objectMapper.readTree(anyString())).thenReturn(objectMapper.createObjectNode());

        WorkflowDefinitionResponse response = service.activateWorkflowDefinition("test-id");

        assertNotNull(response);
        verify(repository, times(1)).save(any(WorkflowDefinitionEntity.class));
    }

    @Test
    void deactivateWorkflowDefinition() throws Exception {
        when(repository.findById(anyString())).thenReturn(Optional.of(entity));
        when(repository.save(any(WorkflowDefinitionEntity.class))).thenReturn(entity);
        when(objectMapper.readTree(anyString())).thenReturn(objectMapper.createObjectNode());

        WorkflowDefinitionResponse response = service.deactivateWorkflowDefinition("test-id");

        assertNotNull(response);
        verify(repository, times(1)).save(any(WorkflowDefinitionEntity.class));
    }
}
