**Commit**: 25031373da · **Ticket**: KAFKA-19154
**Subsystem**: group-coordinator / OffsetMetadataManager
**Category**: Incorrect filtering / wrong query result

**Root cause**
`OffsetMetadataManager.fetchOffsets()` returned a committed offset whenever the offset record existed in the store, without checking whether the stored topic UUID matched the UUID supplied in the request. When a topic is deleted and recreated with a new UUID the old offsets survive until `onPartitionsDeleted` runs, but if the client queries with the new UUID the old offset is returned — making it appear as if the freshly-recreated topic has committed offsets it has never seen.

**Before/After**
```java
// Before — null check only, no UUID comparison
} else if (offsetAndMetadata == null) {
    topicResponse.partitions().add(invalidPartition(...));

// After — also invalidate when topic UUIDs don't match
} else if (isOffsetInvalid(offsetAndMetadata, topic.topicId())) {
    topicResponse.partitions().add(invalidPartition(...));

// Helper:
private static boolean isOffsetInvalid(OffsetAndMetadata m, Uuid expectedId) {
    return m == null || isMismatchedTopicId(m.topicId, expectedId);
}
private static boolean isMismatchedTopicId(Uuid actual, Uuid expected) {
    return !actual.equals(ZERO_UUID) && !expected.equals(ZERO_UUID) && !actual.equals(expected);
}
```

**How to spot**
Whenever a key-value store uses a name/string key but the value contains an additional immutable UUID, a lookup that ignores the UUID will return stale data after a delete-recreate cycle. Always validate the UUID when the protocol supports topic IDs.

**Test**
`OffsetFetchRequestTest.testFetchOffsetWithRecreatedTopic` — commits offsets for a topic, deletes and recreates the topic, then fetches offsets using the new topic ID; asserts that all partitions return `INVALID_OFFSET (-1)`.
