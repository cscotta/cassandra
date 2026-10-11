**Commit**: bc7b87001b · **Ticket**: KAFKA-18676
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Update `benchmark_test.py` to use KRaft. ```
> TC_PATHS="tests/kafkatest/benchmarks/core/benchmark_test.py" /bin/bash tests/docker/run_tests.sh

================================================================================
SESSION REPORT (ALL TESTS)
ducktape version: 0.12.0
session_id:       2025-02-03--001
run time:         96 minutes 48.900 seconds
tests run:        120
passed:           120
flaky:            0
failed:           0
ignored:          0
================================================================================
```

Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
from kafkatest.services.kafka import KafkaService
from kafkatest.services.zookeeper import ZookeeperService
        self.num_zk = 1
        self.zk = ZookeeperService(test_context, self.num_zk)

    def setUp(self):
        self.zk.start()

            self.zk, security_protocol=security_protocol,
    @cluster(num_nodes=5)
    @parametrize(acks=1, topic=TOPIC_REP_ONE)
    @parametrize(acks=1, topic=TOPIC_REP_THREE)

// After
from kafkatest.services.kafka import KafkaService, quorum
            zk=None, security_protocol=security_protocol,
    @cluster(num_nodes=9)
    @parametrize(acks=1, topic=TOPIC_REP_ONE, metadata_quorum=quorum.isolated_kraft)
    @parametrize(acks=1, topic=TOPIC_REP_THREE, metadata_quorum=quorum.isolated_kraft)
    @parametrize(acks=-1, topic=TOPIC_REP_THREE, metadata_quorum=quorum.isolated_kraft)
    @matrix(acks=[1], topic=[TOPIC_REP_THREE], message_size=[10, 100, 1000, 10000, 100000],
            compression_type=["none", "snappy"], security_protocol=['SSL'], tls_version=['TLSv1.2', 'TLSv1.3'], metadata_quorum=quorum.isolated_kraft)
    @matrix(acks=[1], topic=[TOPIC_REP_THREE], message_size=[10, 100, 1000, 10000, 100000],
            compression_type=["none", "snappy"], security_protocol=['PLAINTEXT'], metadata_quorum=quorum.isolated_kraft)
    @parametrize(acks=1, topic=TOPIC_REP_THREE, num_producers=3, metadata_quorum=quorum.isolated_kraft)
                                 broker_version=str(DEV_BRANCH), metadata_quorum=quorum.isolated_kraft):
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18676; update benchmark system tests (#18785).

**Test**
Test modified in benchmark_test.py covering the corrected behaviour.
