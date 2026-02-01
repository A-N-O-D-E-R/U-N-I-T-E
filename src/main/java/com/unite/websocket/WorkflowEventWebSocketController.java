package com.unite.websocket;

import com.unite.dto.WorkflowEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventWebSocketController {

    @MessageMapping("/workflow-events/subscribe")
    @SendTo("/topic/workflow-events")
    public WorkflowEventMessage subscribeToAllEvents() {
        log.info("Client subscribed to all workflow events");
        return WorkflowEventMessage.builder()
                .eventType(WorkflowEventMessage.EventType.STATE_CHANGED)
                .message("Subscribed to workflow events")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @SubscribeMapping("/topic/workflow-events")
    public WorkflowEventMessage onSubscribeToAllEvents() {
        log.info("Client subscribed to /topic/workflow-events");
        return WorkflowEventMessage.builder()
                .eventType(WorkflowEventMessage.EventType.STATE_CHANGED)
                .message("Connected to workflow event stream")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @SubscribeMapping("/topic/workflow-events/{executionId}")
    public WorkflowEventMessage onSubscribeToExecution(@DestinationVariable String executionId) {
        log.info("Client subscribed to execution: {}", executionId);
        return WorkflowEventMessage.builder()
                .executionId(executionId)
                .eventType(WorkflowEventMessage.EventType.STATE_CHANGED)
                .message("Connected to execution event stream")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
