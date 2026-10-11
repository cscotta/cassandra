**Commit**: 14ea1cf61a · **Ticket**: KAFKA-19202
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Enable KIP-1071 in the next system test. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
    def setup_system(self, start_processor=True, num_threads=3):

        self.kafka = KafkaService(self.test_context, num_nodes=self.replication, zk=None, topics=self.topics)
        self.processor1 = StreamsSmokeTestJobRunnerService(self.test_context, self.kafka, "at_least_once", num_threads)
            metadata_quorum=[quorum.combined_kraft])
    def test_broker_type_bounce(self, failure_mode, broker_type, sleep_time_secs, num_threads, metadata_quorum):
        self.setup_system(num_threads=num_threads)
            metadata_quorum=[quorum.combined_kraft])
    def test_broker_type_bounce_at_start(self, failure_mode, broker_type, sleep_time_secs, metadata_quorum):
        self.setup_system(start_processor=False)
            metadata_quorum=[quorum.isolated_kraft])
    def test_many_brokers_bounce(self, failure_mode, num_failures, metadata_quorum):

// After
    def setup_system(self, start_processor=True, num_threads=3, group_protocol='classic'):
        use_streams_groups = True if group_protocol == 'streams' else False
        self.kafka = KafkaService(self.test_context, num_nodes=self.replication, zk=None, topics=self.topics, use_streams_groups=use_streams_groups)
        self.processor1 = StreamsSmokeTestJobRunnerService(self.test_context, self.kafka, "at_least_once", group_protocol=group_protocol, num_threads = num_threads)
            metadata_quorum=[quorum.combined_kraft],
            group_protocol=["classic", "streams"])
    def test_broker_type_bounce(self, failure_mode, broker_type, sleep_time_secs, num_threads, metadata_quorum, group_protocol):
        self.setup_system(num_threads=num_threads, group_protocol=group_protocol)
            metadata_quorum=[quorum.combined_kraft],
            group_protocol=["classic", "streams"])
    def test_broker_type_bounce_at_start(self, failure_mode, broker_type, sleep_time_secs, metadata_quorum, group_protocol):
        self.setup_system(start_processor=False, group_protocol=group_protocol)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19202: enable kip-1071 in streams_broker_bounce_test.py (#19584).

**Test**
Test modified in streams_broker_bounce_test.py covering the corrected behaviour.
