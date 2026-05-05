package com.flowboard.comment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private CommentEventProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        producer = new CommentEventProducer(kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);
    }

    @Test
    void sendCommentAddedPublishesCommentAndMentionEvents() throws Exception {
        producer.sendCommentAdded(11L, 22L, 33L, "hello @john and @jane");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(3)).send(topicCaptor.capture(), payloadCaptor.capture());

        List<String> topics = topicCaptor.getAllValues();
        List<String> payloads = payloadCaptor.getAllValues();

        assertThat(topics).containsExactly(
                "flowboard.comment.added",
                "flowboard.mention.notification",
                "flowboard.mention.notification"
        );

        assertThat(objectMapper.readValue(payloads.get(0), Map.class))
                .containsEntry("commentId", 11)
                .containsEntry("cardId", 22)
                .containsEntry("authorId", 33);

        assertThat(objectMapper.readValue(payloads.get(1), Map.class))
                .containsEntry("username", "john")
                .containsEntry("cardId", 22)
                .containsEntry("actorId", 33);

        assertThat(objectMapper.readValue(payloads.get(2), Map.class))
                .containsEntry("username", "jane")
                .containsEntry("cardId", 22)
                .containsEntry("actorId", 33);
    }

    @Test
    void sendCommentAddedSupportsDotsAndUnderscoresInMentionedUsername() throws Exception {
        producer.sendCommentAdded(11L, 22L, 33L, "ping @john.doe and @jane_doe");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(3)).send(topicCaptor.capture(), payloadCaptor.capture());

        List<String> payloads = payloadCaptor.getAllValues();

        assertThat(objectMapper.readValue(payloads.get(1), Map.class))
                .containsEntry("username", "john.doe")
                .containsEntry("cardId", 22)
                .containsEntry("actorId", 33);

        assertThat(objectMapper.readValue(payloads.get(2), Map.class))
                .containsEntry("username", "jane_doe")
                .containsEntry("cardId", 22)
                .containsEntry("actorId", 33);
    }
}
