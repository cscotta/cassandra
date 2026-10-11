**Commit**: a6bce450dd · **Ticket**: KAFKA-17720
**Subsystem**: tests/kafkatest (ZK migration test)
**Category**: State not cleaned up

**Root cause**
`zookeeper_migration_test.py` tested the ZooKeeper-to-KRaft migration path, which is removed in Kafka 4.0. The `KafkaService` also contained `reconfigure_zk_for_migration` and `reconfigure_zk_as_kraft` helper methods that had no remaining callers. Retaining the test and the helper methods created false confidence that the migration path was still supported and maintained.

**Before/After**
```python
# Before – migration test and helpers present
class ZkMigrationTest(Test):
    def test_online_migration(...): ...

def reconfigure_zk_for_migration(self, ...): ...
def reconfigure_zk_as_kraft(self, ...): ...

# After – file deleted, helpers removed from KafkaService
```

**How to spot**
When a feature is removed from the main codebase, audit the `tests/kafkatest` tree for test files and `KafkaService` helper methods that exist solely to support that feature; leaving them produces a green-looking test suite that exercises a non-existent code path.

**Test**
No test added.
