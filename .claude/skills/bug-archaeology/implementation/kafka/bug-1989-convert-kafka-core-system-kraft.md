**Commit**: 38aca3a045 · **Ticket**: KAFKA-17917
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
- Remove some unused Zookeeper code

- Migrate group mode transactions, security rolling upgrade, and throttling tests to using KRaft

- Add KRaft downgrade tests to kraft_upgrade_test.py

Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
from kafkatest.services.zookeeper import ZookeeperService
        if (authorizer_class == KafkaService.KRAFT_ACL_AUTHORIZER):
            self.zk = None
        else:
            self.zk = ZookeeperService(self.test_context, num_nodes=1)
            self.zk.start()

        self.kafka = KafkaService(self.test_context, num_nodes=1, zk=self.zk,
                                  topics=topics, controller_num_nodes_override=1,
                                  allow_zk_with_kraft=True)
from kafkatest.services.zookeeper import ZookeeperService
        self.zk = ZookeeperService(self.test_context, num_nodes=1) if quorum.for_test(self.test_context) == quorum.zk else None

// After
        self.kafka = KafkaService(self.test_context, num_nodes=1, zk=None,
                                  topics=topics, controller_num_nodes_override=1)
        self.kafka = KafkaService(self.test_context, num_nodes=3, zk=None, version=DEV_BRANCH, topics={self.topic: {
        self.kafka = KafkaService(test_context, num_nodes=3, zk=None)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17917: convert kafka core system tests to use kraft (#17847).

**Test**
Test modified in authorizer_test.py covering the corrected behaviour.
