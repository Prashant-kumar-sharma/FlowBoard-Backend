package com.flowboard.comment.kafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j @Component @RequiredArgsConstructor
public class CommentEventProducer {
    private static final Pattern USERNAME_MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_.]+)");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendCommentAdded(Long commentId, Long cardId, Long authorId, String content) {
        send("flowboard.comment.added", Map.of("commentId", commentId, "cardId", cardId, "authorId", authorId));
        // Extract @mentions
        USERNAME_MENTION_PATTERN.matcher(content).results()
            .forEach(m -> send("flowboard.mention.notification",
                Map.of("username", m.group(1), "cardId", cardId, "actorId", authorId)));
    }

    private void send(String topic, Object payload) {
        try { kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload)); }
        catch (Exception e) { log.error("Kafka error: {}", e.getMessage()); }
    }
}
