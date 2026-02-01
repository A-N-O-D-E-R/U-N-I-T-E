package com.unite.service;

import com.unite.dto.WorkflowEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishEvent(WorkflowEventMessage event) {
        log.debug("Publishing event: {} for execution: {}", event.getEventType(), event.getExecutionId());

        messagingTemplate.convertAndSend("/topic/workflow-events", event);

        messagingTemplate.convertAndSend(
                "/topic/workflow-events/" + event.getExecutionId(),
                event
        );

        log.debug("Event published successfully");
    }

    public void publishEventToExecution(String executionId, WorkflowEventMessage event) {
        log.debug("Publishing event to specific execution: {}", executionId);
        messagingTemplate.convertAndSend("/topic/workflow-events/" + executionId, event);
    }
}
