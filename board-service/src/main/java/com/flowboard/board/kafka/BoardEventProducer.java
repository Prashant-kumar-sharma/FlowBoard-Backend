package com.flowboard.board.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMemberInvited(Long boardId, String boardName, Long invitedUserId, Long invitedByUserId, String role) {
        Map<String, Object> event = Map.of(
                "boardId", boardId,
                "boardName", boardName,
                "invitedUserId", invitedUserId,
                "invitedByUserId", invitedByUserId,
                "role", role
        );
        kafkaTemplate.send("flowboard.board.member.invited", event);
        log.info("Board member invited event sent: userId={} boardId={}", invitedUserId, boardId);
    }
}
