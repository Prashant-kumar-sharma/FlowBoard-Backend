package com.flowboard.card.config;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class NoOpKafkaTemplate extends KafkaTemplate<String, String> {
    public NoOpKafkaTemplate(ProducerFactory<String, String> producerFactory) {
        super(producerFactory);
    }
}
