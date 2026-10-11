**Commit**: 5268fcdc98 · **Ticket**: KAFKA-18678
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
from kafkatest.services.zookeeper import ZookeeperService
        self.zk = ZookeeperService(test_context, num_nodes=1) if quorum.for_test(test_context) == quorum.zk else None
        self.kafka = KafkaService(test_context, num_nodes=1, zk=self.zk,
    def setUp(self):
        if self.zk:
            self.zk.start()

    @cluster(num_nodes=3)
    @matrix(producer_version=[str(DEV_BRANCH)], acks=["0", "1", "-1"], enable_idempotence=[False])
    @matrix(producer_version=[str(DEV_BRANCH)], acks=["-1"], enable_idempotence=[True])
    @matrix(producer_version=[str(DEV_BRANCH)], security_protocol=['PLAINTEXT', 'SSL'], metadata_quorum=quorum.all)
            metadata_quorum=quorum.all)

// After
        self.kafka = KafkaService(test_context, num_nodes=1, zk=None,

    @matrix(producer_version=[str(DEV_BRANCH)], acks=["0", "1", "-1"], enable_idempotence=[False], metadata_quorum=quorum.all_kraft)
    @matrix(producer_version=[str(DEV_BRANCH)], acks=["-1"], enable_idempotence=[True], metadata_quorum=quorum.all_kraft)
    @matrix(producer_version=[str(DEV_BRANCH)], security_protocol=['PLAINTEXT', 'SSL'], metadata_quorum=quorum.all_kraft)
            metadata_quorum=quorum.all_kraft)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18678 update testverifiableproducer system test (#18768).

**Test**
Test modified in test_verifiable_producer.py covering the corrected behaviour.
