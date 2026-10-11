**Commit**: ed9cb08dfe · **Ticket**: KAFKA-17977
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    def __init__(self, context, num_nodes, kafka, topic, group_id="test-consumer-group", new_consumer=True,
            new_consumer:               use new Kafka consumer if True
        self.new_consumer = new_consumer
        if not self.new_consumer:
            args['zk_connect'] = self.kafka.zk_connect_setting()
                cmd += " --feature kraft.version=1"
    def test_compatibility(self, producer_version, consumer_version, compression_types, new_consumer=True, timestamp_type=None, metadata_quorum=quorum.zk):
        if not new_consumer and metadata_quorum != quorum.zk:
            raise Exception("ZooKeeper-based consumers are not supported when using a KRaft metadata quorum")
                                        self.topic, consumer_timeout_ms=30000, new_consum

// After
    def __init__(self, context, num_nodes, kafka, topic, group_id="test-consumer-group",
                        cmd += " --feature kraft.version=1"
    def test_compatibility(self, producer_version, consumer_version, compression_types, timestamp_type=None, metadata_quorum=quorum.zk):
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17977 remove new_consumer from e2e (#17798).

**Test**
Test modified in console_consumer.py covering the corrected behaviour.
