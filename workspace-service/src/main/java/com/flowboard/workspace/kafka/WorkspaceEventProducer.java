package com.flowboard.workspace.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMemberInvited(Long workspaceId, String workspaceName, Long invitedUserId, Long invitedByUserId, String role) {
        Map<String, Object> event = Map.of(
                "workspaceId", workspaceId,
                "workspaceName", workspaceName,
                "invitedUserId", invitedUserId,
                "invitedByUserId", invitedByUserId,
                "role", role
        );
        kafkaTemplate.send("flowboard.workspace.member.invited", event);
        log.info("Workspace member invited event sent: userId={} workspaceId={}", invitedUserId, workspaceId);
    }
}
