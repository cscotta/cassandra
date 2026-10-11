**Commit**: d7a5b877f2 · **Ticket**: KAFKA-18677
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This patch converts the ConsoleConsumerTest system test to only use KRaft. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
from kafkatest.services.zookeeper import ZookeeperService
        self.zk = ZookeeperService(test_context, num_nodes=1) if quorum.for_test(test_context) == quorum.zk else None
        self.kafka = KafkaService(self.test_context, num_nodes=1, zk=self.zk, zk_chroot="/kafka",
    def setUp(self):
        if self.zk:
            self.zk.start()

    @matrix(security_protocol=['SASL_SSL'], sasl_mechanism=['PLAIN'], metadata_quorum=quorum.all_kraft)
    @matrix(security_protocol=['SASL_SSL'], sasl_mechanism=['SCRAM-SHA-256', 'SCRAM-SHA-512']) # SCRAM not yet supported with KRaft

// After
        self.kafka = KafkaService(self.test_context, num_nodes=1, zk=None,
    @matrix(security_protocol=['SASL_SSL'], sasl_mechanism=['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'], metadata_quorum=quorum.all_kraft)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18677; update consoleconsumertest system test (#18763).

**Test**
Test modified in test_console_consumer.py covering the corrected behaviour.
