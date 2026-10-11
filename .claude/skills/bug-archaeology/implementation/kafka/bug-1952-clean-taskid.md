**Commit**: 3f834781a4 · **Ticket**: KAFKA-12844
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
Rename topicGroupId as subtopology. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
 * The task ID representation composed as subtopology (aka topicGroupId) plus the assigned partition ID.
    /** The ID of the subtopology, aka topicGroupId. */
    private final int topicGroupId;
    public TaskId(final int topicGroupId, final int partition) {
        this(topicGroupId, partition, null);
    public TaskId(final int topicGroupId, final int partition, final String topologyName) {
        this.topicGroupId = topicGroupId;
        return topicGroupId;
        return topologyName != null ? topologyName + NAMED_TOPOLOGY_DELIMITER + topicGroupId + "_" + partition : topicGroupId + "_" + partition;
        if (topicGroupId != taskId.topicGroupId || partition != taskId.partition) {
        return Objects.hash(topicGroupId, partition, topologyName);
        final int comparingTopicGroupId = Integer.compare(this.topicGroupId, other.topicGroupId);

// After
 * The task ID representation composed as subtopology plus the assigned partition ID.
    /** The ID of the subtopology. */
    private final int subtopology;
    public TaskId(final int subtopology, final int partition) {
        this(subtopology, partition, null);
    public TaskId(final int subtopology, final int partition, final String topologyName) {
        this.subtopology = subtopology;
        return subtopology;
        return topologyName != null ? topologyName + NAMED_TOPOLOGY_DELIMITER + subtopology + "_" + partition : subtopology + "_" + partition;
        if (subtopology != taskId.subtopology || partition != taskId.partition) {
        return Objects.hash(subtopology, partition, topologyName);
        final int comparingTopicGroupId = Integer.compare(this.subtopology, other.subtopology);
```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12844: clean up taskid (#17904).

**Test**
No test added.
