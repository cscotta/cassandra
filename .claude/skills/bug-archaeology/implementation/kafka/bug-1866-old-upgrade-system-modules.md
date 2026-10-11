**Commit**: 4603f7495e · **Ticket**: KAFKA-18030
**Subsystem**: bin/kafka-run-class.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  if [ "$SHORT_VERSION_NO_DOTS" = "0100" ]; then
    CLASSPATH="/opt/kafka-$UPGRADE_KAFKA_STREAMS_TEST_VERSION/libs/zkclient-0.8.jar":"$CLASSPATH"
    CLASSPATH="/opt/kafka-$UPGRADE_KAFKA_STREAMS_TEST_VERSION/libs/zookeeper-3.4.6.jar":"$CLASSPATH"
  fi
  if [ "$SHORT_VERSION_NO_DOTS" = "0101" ]; then
    CLASSPATH="/opt/kafka-$UPGRADE_KAFKA_STREAMS_TEST_VERSION/libs/zkclient-0.9.jar":"$CLASSPATH"
    CLASSPATH="/opt/kafka-$UPGRADE_KAFKA_STREAMS_TEST_VERSION/libs/zookeeper-3.4.8.jar":"$CLASSPATH"
  fi
            ':streams:upgrade-system-tests-0100:test',
            ':streams:upgrade-system-tests-0101:test',
            ':streams:upgrade-system-tests-0102:test',
project(':streams:upgrade-system-tests-0100') {

// After
// (no additions)
```

**How to spot**
When reviewing bin/kafka-run-class.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18030 remove old upgrade-system-tests modules (#17843).

**Test**
Test modified in StreamsUpgradeTest.java covering the corrected behaviour.
