**Commit**: 4377ee0808 · **Ticket**: KAFKA-19421
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
`streams_broker_down_resilience_test` produce messages with `null` key
to a topic with three partitions and expect each partition to be
non-empty afterward. But I don't think this is a correct assumption, as
a producer may try to be sticky and only produce to two partitions. This cause occasional flakiness in the test. The fix is to produce records with keys.

**Before/After**
```java
// Before
    def assert_produce(self, topic, test_state, num_messages=5, timeout_sec=60):
        producer = self.get_producer(topic, num_messages)
                                        timeout_sec=120)
                                        timeout_sec=120)
                                        timeout_sec=120)
                                        timeout_sec=120)

// After
    def assert_produce(self, topic, test_state, num_messages=5, timeout_sec=60, repeating_keys=None):
        producer = self.get_producer(topic, num_messages, repeating_keys=repeating_keys)
                    # repeating_keys enables production of records with keys, ensuring that we produce to all 3 partitions
                                        timeout_sec=120,
                                        repeating_keys=self.num_messages)
                    # repeating_keys enables production of records with keys, ensuring that we produce to all 3 partitions
                                        timeout_sec=120,
                                        repeating_keys=self.num_messages)
                    # repeating_keys enables production of records with keys, ensuring that we produce to all 3 partitions
                                        timeout_sec=120,
                                        repeating_keys=self.num_messages)
                    # repeating_keys enables production of records with keys, ensuring that we produce to all 3 partitions
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19421: deflake streams_broker_down_resilience_test (#19999).

**Test**
Test modified in base_streams_test.py covering the corrected behaviour.
