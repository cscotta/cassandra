**Commit**: cf5a86b654 · **Ticket**: KAFKA-16890
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
The listRemoteLogSegments returns the metadata list sorted by the start-offset. However, the returned metadata list contains all the uploaded segment information including the duplicate and overlapping remote-log-segments. The reason for duplicate/overlapping remote-log-segments cases is explained [here](https://github.com/apache/kafka/blob/trunk/storage/src/main/java/org/apache/kafka/server/log/remote/metadata/storage/RemoteLogLeaderEpochState.java#L103). The list returned by the RLMM#listRemoteLogSegments can contain the duplicate segment metadata at the end of the list.

**Before/After**
```java
// Before
                    logStartOffset = OptionalLong.of(metadata.endOffset() + 1);
                    logStartOffset = OptionalLong.of(metadata.endOffset() + 1);

// After
                    if (!logStartOffset.isPresent() || logStartOffset.getAsLong() < metadata.endOffset() + 1) {
                        logStartOffset = OptionalLong.of(metadata.endOffset() + 1);
                    }
                    if (!logStartOffset.isPresent() || logStartOffset.getAsLong() < metadata.endOffset() + 1) {
                        logStartOffset = OptionalLong.of(metadata.endOffset() + 1);
                    }
    @ParameterizedTest(name = "testDeletionOnOverlappingRetentionBreachedSegments retentionSize={0} retentionMs={1}")
    @CsvSource(value = {"0, -1", "-1, 0"})
    public void testDeletionOnOverlappingRetentionBreachedSegments(long retentionSize,
                                                                   long retentionMs)
            throws RemoteStorageException, ExecutionException, InterruptedException {
        Map<String, Long> logProps = new HashMap<>();
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16890: compute valid log-start-offset when deleting overlapping remote segments (#16237).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
