#!/usr/bin/env bash
set -euo pipefail

bootstrap="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
topics=(
  "policymesh.policy.updated"
  "policymesh.decision.created"
  "policymesh.lineage.created"
  "policymesh.ci.completed"
)

until /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$bootstrap" --list >/dev/null 2>&1; do
  echo "Waiting for Kafka at $bootstrap..."
  sleep 2
done

for topic in "${topics[@]}"; do
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$bootstrap" --create --if-not-exists \
    --topic "$topic" --partitions 1 --replication-factor 1 \
    --config retention.ms=604800000
done

echo "PolicyMesh topics are ready."
