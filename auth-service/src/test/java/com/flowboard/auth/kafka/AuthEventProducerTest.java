package com.flowboard.auth.kafka;

import com.flowboard.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuthEventProducer producer;
    private User user;

    @BeforeEach
    void setUp() {
        producer = new AuthEventProducer(kafkaTemplate);
        user = User.builder().id(1L).email("alice@test.com").fullName("Alice").build();
    }

    @Test
    void sendAccountSuspendedPublishesSuspendedEvent() {
        producer.sendAccountSuspended(user);

        ArgumentCaptor<Map<String, Object>> captor = mapCaptor();
        verify(kafkaTemplate).send(eq("flowboard.account.status.changed"), captor.capture());
        assertThat(captor.getValue()).containsEntry("status", "SUSPENDED");
    }

    @Test
    void sendAccountRestoredPublishesRestoredEvent() {
        producer.sendAccountRestored(user);

        ArgumentCaptor<Map<String, Object>> captor = mapCaptor();
        verify(kafkaTemplate).send(eq("flowboard.account.status.changed"), captor.capture());
        assertThat(captor.getValue()).containsEntry("status", "RESTORED");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }
}
