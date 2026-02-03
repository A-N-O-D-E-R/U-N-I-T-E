package com.unite.service.workflows;

import java.time.LocalDateTime;

import com.anode.workflow.entities.events.EventType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.EventHandler;
import com.anode.workflow.spring.autoconfigure.annotations.WorkflowEventHandler;
import com.unite.dto.WorkflowEventMessage;
import com.unite.mapper.WorkflowExecutionMapper;
import com.unite.model.WorkflowExecutionEntity;

@WorkflowEventHandler
public class UniteEventHandler implements EventHandler{

    @Override
    public void invoke(EventType event, WorkflowContext context) {
        executionRepository.save(WorkflowExecutionMapper.map(context));

        WorkflowEventMessage event = WorkflowEventMessage.builder()
                    .executionId(context.getCaseId())
                    .workflowDefinitionId(definitionId)
                    .eventType(event)
                    .message(message)
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishEvent(event);
    }

    
}
