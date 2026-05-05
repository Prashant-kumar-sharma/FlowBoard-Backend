package com.flowboard.notification.config;

import org.springframework.context.annotation.Configuration;

/**
 * Kafka producer wiring is intentionally disabled while Kafka auto-configuration is excluded.
 */
@Configuration
public class KafkaProducerConfig {
    // No producer beans required while Kafka is disabled.
}
