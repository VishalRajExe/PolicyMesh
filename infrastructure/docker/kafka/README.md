# Kafka container

The local broker uses KRaft (no ZooKeeper) with one combined controller/broker. It advertises `kafka:9092` to Compose services and `localhost:9092` to host tools. `topics.sh` is run by the one-shot `kafka-init` service; `--if-not-exists` makes it safe to rerun.
