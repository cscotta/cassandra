**Commit**: 7fef5b8646 · **Ticket**: KAFKA-18763
**Subsystem**: tests/kafkatest
**Category**: Resource leak

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        self.total_acknowledged = 0
            self.total_acknowledged += event["count"]
    def total_successful_acknowledged(self):
            return self.event_handlers[node].total_acknowledged + self.event_handlers[node].total_acknowledged_failed
    def total_successful_acknowledged_for_a_share_consumer(self, node):
            return self.event_handlers[node].total_acknowledged
        assert consumer.total_acknowledged() == producer.num_acked
            assert event_handler.total_acknowledged > 0
        assert consumer.total_acknowledged() == producer.num_acked
            assert event_handler.total_acknowledged > 0

// After
        self.total_acknowledged_successfully = 0
            self.total_acknowledged_successfully += event["count"]
    def total_acknowledged_successfully(self):
            return self.event_handlers[node].total_acknowledged_successfully + self.event_handlers[node].total_acknowledged_failed
    def total_acknowledged_sucessfully_for_a_share_consumer(self, node):
            return self.event_handlers[node].total_acknowledged_successfully
        assert consumer.total_acknowledged_successfully() == producer.num_acked
            assert event_handler.total_acknowledged_successfully > 0
        assert consumer.total_acknowledged_successfully() == producer.num_acked
            assert event_handler.total_acknowledged_successfully > 0
```

**How to spot**
When reviewing tests/kafkatest code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18763: changed the assertion statement for acknowledgements to include only successful acks (#18846).

**Test**
Test modified in verifiable_share_consumer.py covering the corrected behaviour.
