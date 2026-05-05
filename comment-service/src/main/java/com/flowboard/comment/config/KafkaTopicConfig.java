package com.flowboard.comment.config;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean public NewTopic cardAssigned() { return TopicBuilder.name("flowboard.card.assigned").partitions(3).replicas(1).build(); }
    @Bean public NewTopic cardMoved() { return TopicBuilder.name("flowboard.card.moved").partitions(3).replicas(1).build(); }
    @Bean public NewTopic cardOverdue() { return TopicBuilder.name("flowboard.card.overdue").partitions(3).replicas(1).build(); }
    @Bean public NewTopic commentAdded() { return TopicBuilder.name("flowboard.comment.added").partitions(3).replicas(1).build(); }
    @Bean public NewTopic mentionNotification() { return TopicBuilder.name("flowboard.mention.notification").partitions(3).replicas(1).build(); }
    @Bean public NewTopic dueDateReminder() { return TopicBuilder.name("flowboard.due-date.reminder").partitions(3).replicas(1).build(); }
}