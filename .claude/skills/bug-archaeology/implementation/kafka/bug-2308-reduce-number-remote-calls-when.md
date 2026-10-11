**Commit**: 40360819bb · **Ticket**: KAFKA-17498
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
While serving  LIST_OFFSETS request, avoid fetching the remote indexes if they can be served from the local indexes. Reviewers: Satish Duggana <satishd@apache.org>, Luke Chen <showuon@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
    private Optional<FileRecords.TimestampAndOffset> lookupTimestamp(RemoteLogSegmentMetadata rlsMetadata, long timestamp, long startingOffset)
     * Search the message offset in the remote storage based on timestamp and offset.
     * <p>



                    return lookupTimestamp(rlsMetadata, timestamp, startingOffset);


        brokerTopicStats = new BrokerTopicStats(KafkaConfig.fromProps(props).remoteLogManagerConfig().isRemoteStorageSystemEnabled());

// After
    Optional<FileRecords.TimestampAndOffset> lookupTimestamp(RemoteLogSegmentMetadata rlsMetadata, long timestamp, long startingOffset)
     * Search the message offset in the remote storage for the given timestamp and starting-offset.
     * Once the target segment where the search to be performed is found:
     * 1. If the target segment lies in the local storage (common segments that lies in both remote and local storage),
     *    then the search will be performed in the local storage.
     * 2. If the target segment is found only in the remote storage, then the search will be performed in the remote storage.
     *
     *  <p>
                    // cache to avoid race conditions
                    List<LogSegment> segmentsCopy = new ArrayList<>(unifiedLog.logSegments());
                    if (segmentsCopy.isEmpty() || rlsMetadata.startOffset() < segmentsCopy.get(0).baseOffset()) {
                        // search in remote-log
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17498: reduce the number of remote calls when serving list_offsets request (#17132).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
