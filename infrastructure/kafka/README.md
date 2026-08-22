# Kafka ownership

Infrastructure creates the documented topics. The backend publishes policy, decision, and lineage events; consumers may be added later. The CI checker is a CLI tool, not a broker-side service. Run `docker compose ... run --rm kafka-init` to re-assert topics after bringing up Kafka.
