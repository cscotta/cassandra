**Commit**: f5b8891b0c · **Ticket**: KAFKA-19202
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Enable KIP-1071 in the next system test. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    def get_configs(extra_configs=""):
        updated_configs = consumer_poll_ms + "," + retries_config + "," + request_timeout + "," + max_block_ms + extra_configs
    @matrix(metadata_quorum=[quorum.combined_kraft])
    def test_streams_resilient_to_broker_down(self, metadata_quorum):
        processor = StreamsBrokerDownResilienceService(self.test_context, self.kafka, self.get_configs())
    @matrix(metadata_quorum=[quorum.combined_kraft])
    def test_streams_runs_with_broker_down_initially(self, metadata_quorum):
        configs = self.get_configs(extra_configs=",application.id=starting_wo_broker_id")
    @matrix(metadata_quorum=[quorum.combined_kraft])
    def test_streams_should_scale_in_while_brokers_down(self, metadata_quorum):
            extra_configs=",application.id=shutdown_with_broker_down" +
                          ",internal.task.assignor.class=org.apache.kafka.streams.processor.internals.assignment.LegacyStickyTaskAssignor"

// After
        super(BaseStreamsTest, self).__init__(test_context = test_context)
    def get_configs(group_protocol="classic", extra_configs=""):
        group_protocol = "group.protocol=" + group_protocol
        updated_configs = consumer_poll_ms + "," + retries_config + "," + request_timeout + "," + max_block_ms + "," + group_protocol + extra_configs
    @matrix(metadata_quorum=[quorum.combined_kraft],
            group_protocol=["classic", "streams"])
    def test_streams_resilient_to_broker_down(self, metadata_quorum, group_protocol):
        processor = StreamsBrokerDownResilienceService(self.test_context, self.kafka, self.get_configs(
            group_protocol=group_protocol))
    @matrix(metadata_quorum=[quorum.combined_kraft],
            group_protocol=["classic", "streams"])
    def test_streams_runs_with_broker_down_initially(self, metadata_quorum, group_protocol):
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19202: enable kip-1071 in streams_broker_down_resilience_test (#19594).

**Test**
Test modified in base_streams_test.py covering the corrected behaviour.
