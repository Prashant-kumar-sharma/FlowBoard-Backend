package com.flowboard.auth.kafka;

import com.flowboard.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendAccountSuspended(User user) {
        sendAccountStatusChanged(user, "SUSPENDED");
    }

    public void sendAccountRestored(User user) {
        sendAccountStatusChanged(user, "RESTORED");
    }

    private void sendAccountStatusChanged(User user, String status) {
        Map<String, Object> event = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "status", status
        );
        kafkaTemplate.send("flowboard.account.status.changed", event);
        log.info("Account status event sent: userId={} status={}", user.getId(), status);
    }
}
