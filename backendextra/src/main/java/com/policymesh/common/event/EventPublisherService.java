package com.policymesh.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes domain events to Kafka asynchronously. Never blocks or fails
 * the calling request: if Kafka is disabled or unreachable, the event is
 * simply logged and dropped so the rest of the system keeps working
 * (per requirement: Kafka is not required for correctness).
 */
@Slf4j
@Service
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;

    public EventPublisherService(KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("${policymesh.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publish(String topic, String key, Object payload) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled; skipping publish to {}", topic);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, payload).exceptionally(ex -> {
                log.warn("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                return null;
            });
        } catch (Exception ex) {
            log.warn("Kafka publish skipped (broker unavailable?) topic={} error={}", topic, ex.getMessage());
        }
    }
}
