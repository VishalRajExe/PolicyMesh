package com.policymesh.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the PolicyMesh event topics. Kafka is asynchronous
 * infrastructure only — the application must remain fully usable in a
 * "simplified local mode" if the broker is not reachable (see
 * EventPublisherService for the defensive publish logic).
 */
@Configuration
public class KafkaConfig {

    public static final String TOPIC_POLICY_UPDATED = "policymesh.policy.updated";
    public static final String TOPIC_DECISION_CREATED = "policymesh.decision.created";
    public static final String TOPIC_LINEAGE_CREATED = "policymesh.lineage.created";
    public static final String TOPIC_CI_COMPLETED = "policymesh.ci.completed";

    @Bean
    public NewTopic policyUpdatedTopic() {
        return TopicBuilder.name(TOPIC_POLICY_UPDATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic decisionCreatedTopic() {
        return TopicBuilder.name(TOPIC_DECISION_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic lineageCreatedTopic() {
        return TopicBuilder.name(TOPIC_LINEAGE_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ciCompletedTopic() {
        return TopicBuilder.name(TOPIC_CI_COMPLETED).partitions(1).replicas(1).build();
    }
}
