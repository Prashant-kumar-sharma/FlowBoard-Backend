package com.flowboard.card.kafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j @Component @RequiredArgsConstructor
public class CardEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendCardAssigned(Long cardId, Long assigneeId, Long actorId) {
        send("flowboard.card.assigned", Map.of("cardId", cardId, "assigneeId", assigneeId, "actorId", actorId));
    }

    public void sendCardMoved(Long cardId, Long fromListId, Long toListId, Long actorId) {
        send("flowboard.card.moved", Map.of("cardId", cardId, "fromListId", fromListId, "toListId", toListId, "actorId", actorId));
    }

    private void send(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, json);
            log.debug("Sent Kafka event to {}: {}", topic, json);
        } catch (Exception e) {
            log.error("Failed to send Kafka event to {}: {}", topic, e.getMessage());
        }
    }
}