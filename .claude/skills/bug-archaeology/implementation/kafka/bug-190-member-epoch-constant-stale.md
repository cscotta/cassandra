**Commit**: a76e58a872 · **Ticket**: KAFKA-20268
**Subsystem**: group-coordinator/src/test/java/org/apache/kafka/coordinator/group/GroupMetadataManagerTest.java
**Category**: Wrong constant / default value

**Root cause**: KAFKA-20268 changed group initialization to start at epoch 1 instead of epoch 0, making the first observed member epoch 2. Tests added by KAFKA-20162 were written against the old epoch-0 initialization and therefore hard-coded expected member epochs as 1. After the KAFKA-20268 merge these assertions failed.

**Before/After**:
```java
// BEFORE – expected epoch 1 (stale after KAFKA-20268)
assertEquals(1, result.response().memberEpoch());

// AFTER – corrected to epoch 2
assertEquals(2, result.response().memberEpoch());
```

**How to spot**: Epoch constants in group-coordinator tests are tightly coupled to initialization logic. When a commit changes the starting epoch, search for all `assertEquals(<small_int>, .*memberEpoch())` assertions in `GroupMetadataManagerTest` — they are likely stale.

**Test**: The tests (consumer group and share group heartbeat tests) now assert `memberEpoch() == 2` for the first response, matching the KAFKA-20268 initialization behavior. No new scenario is added; this is purely a constant correction.
