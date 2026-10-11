**Commit**: 88a73c35e7 · **Ticket**: KAFKA-19423
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
The test is resizing the `__consumer_offset` topic after broker start. This seems to be completely unsupported. The group coordinator fetches
the number of partitions for the consumer offset topic once and never
updates it. So we can be in a state where two brokers have a different
understanding of how `__consumer_offsets` are partitioned.

**Before/After**
```java
// Before
            '__consumer_offsets' : { 'partitions': 50, 'replication-factor': self.replication,
                       'configs': {"min.insync.replicas": 2} }
        self.kafka = KafkaService(self.test_context, num_nodes=self.replication, zk=None, topics=self.topics, use_streams_groups=use_streams_groups)
        self.topics['__consumer_offsets'] = { 'partitions': 50, 'replication-factor': self.replication,

// After
            '__consumer_offsets' : { 'partitions': self.partitions, 'replication-factor': self.replication,
                                     'configs': {"min.insync.replicas": 2} }
        self.kafka = KafkaService(self.test_context, num_nodes=self.replication, zk=None, topics=self.topics, server_prop_overrides=[
            ["offsets.topic.num.partitions", self.partitions],
            ["offsets.topic.replication.factor", self.replication]
        ], use_streams_groups=use_streams_groups)
        self.topics['__consumer_offsets'] = { 'partitions': self.partitions, 'replication-factor': self.replication,
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19423: deflake streams_broker_bounce_test (#20005).

**Test**
Test modified in streams_broker_bounce_test.py covering the corrected behaviour.
