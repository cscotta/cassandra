**Commit**: 453cf9c987 · **Ticket**: KAFKA-17434
**Subsystem**: tests/kafkatest/tests/core/upgrade_test.py
**Category**: Logic error in condition

**Root cause**
KIP-902 upgraded ZooKeeper to 3.8.2, which is incompatible with Kafka versions earlier than 2.4. The upgrade and downgrade system tests still included parametrize cases for versions 0.8.2 through 2.3, which would fail when the test environment used ZK 3.8.2. These tests passed before because they used trivial (no-topic-creation) scenarios that never exposed the ZK incompatibility, but they represented impossible and untested upgrade paths that gave false confidence.

**Before/After**
```python
# Before (included versions < 2.4 which are ZK-incompatible)
@parametrize(from_kafka_version=str(LATEST_0_8_2), ...)
@parametrize(from_kafka_version=str(LATEST_0_9), ...)
...
@parametrize(from_kafka_version=str(LATEST_2_3), ...)

# After (starts from 2.4 which is ZK 3.8 compatible)
@parametrize(from_kafka_version=str(LATEST_2_4), ...)
```

**How to spot**
Whenever a dependency (like ZooKeeper) is upgraded with an incompatible version change, audit all system test parametrize decorators that cover version ranges crossing the incompatibility boundary. Tests that skip topic creation or produce/consume during the upgrade may pass spuriously even though the tested upgrade path is no longer supported.

**Test**
Old parametrize cases for versions < 2.4 removed from both `upgrade_test.py` and `downgrade_test.py`.
