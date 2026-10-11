**Commit**: 1c3ee7fe60 · **Ticket**: KAFKA-18004
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
We plan to remove all ZooKeeper-related code in version 4.0. However, some old brokers in the end-to-end tests still require ZooKeeper service, so we need to run the ZooKeeper service using the 3.x release instead of the dev branch. Since version 3.9 is not available in the https://s3-us-west-2.amazonaws.com/kafka-packages repo, we can use version 3.8 for now. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
from kafkatest.version import DEV_BRANCH
                 zk_tls_encrypt_only = False, version=DEV_BRANCH):
        kafka_run_class = self.path.script("kafka-run-class.sh", DEV_BRANCH)
        kafka_run_class = self.path.script("kafka-run-class.sh", DEV_BRANCH)
        kafka_run_class = self.path.script("kafka-run-class.sh", DEV_BRANCH)
        kafka_run_class = self.path.script("kafka-run-class.sh", DEV_BRANCH)
        kafka_run_class = self.path.script("kafka-run-class.sh", DEV_BRANCH)

// After
from kafkatest.version import LATEST_3_8
    # After 4.0, zookeeper service is removed from source code. Using LATEST_3_8 for compatibility test cases.
                 zk_tls_encrypt_only = False, version=LATEST_3_8):
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18004 use version 3.8 to run the zookeeper service for end-to-end tests (#17790).

**Test**
Test modified in zookeeper.py covering the corrected behaviour.
