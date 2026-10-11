**Commit**: cdbb8fa4ce · **Ticket**: KAFKA-18629
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* When deleting share groups, only initialized and initializing
information in the metadata record is considered. However, it could
happen that the deleting topics also contains info due to other RPCs
(share group offsets delete). * We need to account for existing information while writing the metadata
record in the delete flow. * This PR aims to add the impl for the same.

**Before/After**
```java
// Before
    private void addInitializingTopicsRecords(String groupId, List<CoordinatorRecord> records, Map<Uuid, Set<Integer>> topicPartitionMap) {
                attachTopicName(currentMap.deletingTopics())
        // Remove all initializing and initialized topic info from record and add deleting.
    public void testShareGroupDeleteRequest() {

// After
    // Visibility for tests
    void addInitializingTopicsRecords(String groupId, List<CoordinatorRecord> records, Map<Uuid, Set<Integer>> topicPartitionMap) {
        // If any initializing topics are also present in the deleting state
        // we should remove them from deleting.
        Set<Uuid> currentDeleting = new HashSet<>(currentMap.deletingTopics());
        if (!currentDeleting.isEmpty()) {
            finalInitializingMap.keySet().forEach(key -> {
                if (currentDeleting.remove(key)) {
                    log.warn("Initializing topic {} for share group {} found in deleting state as well, removing from deleting.", metadataImage.topics().getTopic(key).name(), groupId);
                }
            });
        }
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18629: account for existing deleting topics in share group delete. (#19463).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
