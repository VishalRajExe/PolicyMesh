package com.policymesh.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Async Kafka notifications (docs/KAFKA.md). Events are never on the critical path:
 * publication failures are logged and swallowed, and the whole channel can be disabled
 * with policymesh.kafka.enabled=false for simplified local development.
 */
@Component
public class EventPublisher {
  public static final String TOPIC_POLICY_UPDATED = "policymesh.policy.updated";
  public static final String TOPIC_DECISION_CREATED = "policymesh.decision.created";
  public static final String TOPIC_LINEAGE_CREATED = "policymesh.lineage.created";
  public static final String TOPIC_CI_COMPLETED = "policymesh.ci.completed";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EventPublisher.class);

  private final ObjectProvider<KafkaTemplate<String, String>> kafka;
  private final ObjectMapper mapper;
  private final boolean enabled;

  public EventPublisher(ObjectProvider<KafkaTemplate<String, String>> kafka,
                        ObjectMapper mapper,
                        @Value("${policymesh.kafka.enabled:false}") boolean enabled) {
    this.kafka = kafka;
    this.mapper = mapper;
    this.enabled = enabled;
  }

  public void publish(String topic, Map<String, Object> payload) {
    if (!enabled) return;
    try {
      KafkaTemplate<String, String> template = kafka.getIfAvailable();
      if (template != null) template.send(topic, mapper.writeValueAsString(payload));
    } catch (RuntimeException | java.io.IOException e) {
      log.warn("Failed to publish {} event: {}", topic, e.getMessage());
    }
  }
}
