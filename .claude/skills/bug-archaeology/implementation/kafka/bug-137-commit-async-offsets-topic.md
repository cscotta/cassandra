**Commit**: 6826f45fd8 · **Ticket**: KAFKA-19352
**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/PlaintextConsumerCommitTest.java
**Category**: State not cleaned up

**Root cause**
`testCommitAsyncCompletedBeforeConsumerCloses` was flaky because the offsets topic (`__consumer_offsets`) had not been created before the test ran. Without the offsets topic the group coordinator is unavailable, so the commit async never completes and `consumer.close()` times out waiting for it. The fix explicitly pre-creates the offsets topic in the test setup, ensuring the coordinator is reachable. The ticket notes that the actual root cause (KAFKA-19357) was addressed separately.

**Before/After**
```java
// BEFORE – no offsets topic creation; flaky when coordinator not ready
@ClusterTest
public void testCommitAsyncCompletedBeforeConsumerCloses() { ... }

// AFTER – offsets topic created first
@ClusterTest
public void testCommitAsyncCompletedBeforeConsumerCloses() throws InterruptedException {
    cluster.createTopic(Topic.GROUP_METADATA_TOPIC_NAME,
        Integer.parseInt(OFFSETS_TOPIC_PARTITIONS),
        Short.parseShort(OFFSETS_TOPIC_REPLICATION));
    ...
}
```

**How to spot**
Integration tests that rely on the group coordinator without explicitly creating `__consumer_offsets` can flake on clusters where auto-creation is slow or disabled. Search for consumer integration tests that call `commitAsync` or `commitSync` without a preceding coordinator-availability check.

**Test**
This commit is itself a test fix. The assertion is that `commitAsync` completes before `consumer.close()` returns, and that the test is no longer flaky (verified by the author running it 100 times).
