**Commit**: df93571f50 · **Ticket**: KAFKA-19338
**Subsystem**: share-coordinator / ShareCoordinatorShard
**Category**: State not cleaned up

**Root cause**
`ShareCoordinatorShard.readState()` and `writeState()` accepted requests for share partitions that had never been initialized via `initializeState()`. For an uninitialized partition, the `shareStateMap` did not contain the key, so `readState()` silently returned an empty success response (start offset −1, no batches) and `writeState()` created a new, unanchored entry. Operations on uninitialized partitions should be rejected with an explicit error; only `initializeState`, `readSummary`, and `alter` are permitted before initialization.

**Before/After**
```java
// Before — readState() silently succeeded for uninitialized partitions
if (!shareStateMap.containsKey(key)) {
    responseData = ReadShareGroupStateResponse.toResponseData(topicId, partitionId,
        UNINITIALIZED_START_OFFSET, DEFAULT_STATE_EPOCH, List.of());
}

// After — explicit error constants added; callers return error responses
public static final Exception WRITE_UNINITIALIZED_SHARE_PARTITION =
    new Exception("Write operation on uninitialized share partition not allowed.");
public static final Exception READ_UNINITIALIZED_SHARE_PARTITION =
    new Exception("Read operation on uninitialized share partition not allowed.");
```

**How to spot**
Any coordinator or manager that maintains per-partition state maps should verify that the key is present (i.e., the partition was initialized) before processing read or write requests. A missing-key "success" disguises a client bug as a benign no-op.

**Test**
`ShareCoordinatorShardTest.testReadStateUninitializedRecord` / `testWriteStateUninitializedRecord` — send read/write requests without first calling `initializeState` and assert the response contains the expected error code.
