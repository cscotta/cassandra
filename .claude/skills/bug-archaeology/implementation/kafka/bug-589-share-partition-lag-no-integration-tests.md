**Commit**: 3fd4892456 · **Ticket**: KAFKA-19860

**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/ShareConsumerTest.java

**Category**: Missing null/bounds check

**Root cause**
KIP-1226 added share-partition lag persistence and retrieval but lacked integration-test coverage. Without end-to-end tests, regressions in the `ListShareGroupOffsets` admin API response or in the lag computation within the share coordinator could go undetected. The PR adds several scenarios: lag after initial consumption, lag after re-delivery, lag when no consumer is active, and lag when the topic is deleted, exercising the full lifecycle of the `SharePartitionOffsetInfo.lag()` field.

**Before**
```java
// Before: no integration tests for share-partition lag
```

**After**
```java
// After: multiple lag scenarios tested
ListShareGroupOffsetsResult result =
    admin.listShareGroupOffsets(Map.of("group1",
        new ListShareGroupOffsetsSpec()));
Map<TopicPartition, SharePartitionOffsetInfo> offsets =
    result.partitionsToOffsetInfo().get().get("group1");
assertNull(offsets.get(tp).lag().orElse(null)); // before any consumption
```

**How to spot**
Search `ShareConsumerTest` for tests that call `admin.listShareGroupOffsets`; if none exercise the lag field through a full produce-consume-release cycle, the lag computation is undertested.

**Test**
Run the newly added lag integration tests and assert that `SharePartitionOffsetInfo.lag()` returns the expected value at each stage of the consumption lifecycle.
