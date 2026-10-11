**Commit**: b9976437e1 · **Ticket**: KAFKA-16780
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
- KIP-1058 (https://cwiki.apache.org/confluence/display/KAFKA/KIP-1058:+Txn+consumer+exerts+pressure+on+remote+storage+when+collecting+aborted+transactions)
- Unit and Integration tests added. Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before

                    segmentLeaderEpochs);
            logger.info("Copied {} to remote storage with segment-id: {}", logFileName, copySegmentFinishedRlsm.remoteLogSegmentId());

// After
    /**
     * Returns the next segment that may contain the aborted transaction entries. The search ensures that the returned
     * segment offsets are greater than or equal to the given offset and in the same epoch.
     * @param topicPartition topic partition to search
     * @param epochForOffset the epoch
     * @param offset the offset
     * @return The next segment that contains the transaction index in the same epoch.
     * @throws RemoteStorageException If an error occurs while fetching the remote log segment metadata.
     */
    public Optional<RemoteLogSegmentMetadata> fetchNextSegmentWithTxnIndex(TopicPartition topicPartition,
                                                                           int epochForOffset,
                                                                           long offset) throws RemoteStorageException {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16780: txn consumer exerts pressure on remote storage when collecting aborted txns (#17659).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
