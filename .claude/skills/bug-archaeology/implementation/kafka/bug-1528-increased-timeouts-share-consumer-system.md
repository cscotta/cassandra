**Commit**: c09d2c940f · **Ticket**: KAFKA-18642
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    def wait_until_topic_replicas_settled(self, topic, expected_num_isr, timeout_sec=60):
    def wait_until_topic_partition_leaders_settled(self, topic, timeout_sec=60):
    def rolling_bounce_brokers(self, topic, num_bounces=5, clean_shutdown=True, timeout_sec=60):
    def fail_brokers(self, topic, num_brokers=1, clean_shutdown=True, timeout_sec=60):
    def rolling_bounce_share_consumers(self, consumer, keep_alive=0, num_bounces=5, clean_shutdown=True, timeout_sec=60):
    def bounce_all_share_consumers(self, consumer, keep_alive=0, num_bounces=5, clean_shutdown=True, timeout_sec=60):
    def fail_share_consumers(self, consumer, num_consumers=1, clean_shutdown=True, timeout_sec=60):
        self.await_all_members(consumer, timeout_sec=60)
        self.await_acknowledged_messages(consumer, min_messages=total_messages, timeout_sec=60)
        self.await_all_members(consumer, timeout_sec=60)

// After
    default_timeout_sec = 600

    def wait_until_topic_replicas_settled(self, topic, expected_num_isr, timeout_sec=default_timeout_sec):
    def wait_until_topic_partition_leaders_settled(self, topic, timeout_sec=default_timeout_sec):
    def rolling_bounce_brokers(self, topic, num_bounces=5, clean_shutdown=True, timeout_sec=default_timeout_sec):
    def fail_brokers(self, topic, num_brokers=1, clean_shutdown=True, timeout_sec=default_timeout_sec):
    def rolling_bounce_share_consumers(self, consumer, keep_alive=0, num_bounces=5, clean_shutdown=True, timeout_sec=default_timeout_sec):
    def bounce_all_share_consumers(self, consumer, keep_alive=0, num_bounces=5, clean_shutdown=True, timeout_sec=default_timeout_sec):
    def fail_share_consumers(self, consumer, num_consumers=1, clean_shutdown=True, timeout_sec=default_timeout_sec):
        self.await_all_members(consumer, timeout_sec=self.default_timeout_sec)
        self.await_acknowledged_messages(consumer, min_messages=total_messages, timeout_sec=self.default_timeout_sec)

```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18642: increased the timeouts in share_consumer_test.py system tests (#18699).

**Test**
Test modified in share_consumer_test.py covering the corrected behaviour.
