package com.flowboard.notification.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Kafka consumers are disabled while Kafka is not available.
 * This class is a no-op stub replacing the original NotificationConsumer.
 */
@Slf4j
@Component
public class NotificationConsumer {
    // All Kafka listeners disabled - Kafka broker not available
}