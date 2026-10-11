**Commit**: f188a31124 · **Ticket**: KAFKA-19500
**Subsystem**: tools/src/main/java/org/apache/kafka/tools/consumer/group/ConsumerGroupCommand.java
**Category**: Incorrect filtering / wrong query result

**Root cause**
`kafka-consumer-groups.sh --reset-offsets --execute` called `prepareOffsetsToReset()` without first verifying that all requested topic-partitions exist and have a live leader. When a partition leader was unavailable the tool would silently produce incorrect offsets or hang while waiting for metadata, rather than failing immediately with an actionable error. The fix adds `checkAllTopicPartitionsValid()` as the first step, which calls `AdminClient.describeTopics()` to verify existence and inspects each partition's leader node for `Node.noNode()`, throwing `UnknownTopicOrPartitionException` or `LeaderNotAvailableException` respectively before any offset calculation begins.

**Before/After**
```java
// BEFORE – no validation; invalid/leaderless partitions silently cause issues
private Map<TopicPartition, OffsetAndMetadata> prepareOffsetsToReset(...) {
    if (opts.options.has(opts.resetToOffsetOpt)) { ... }
    ...
}

// AFTER – validation first
private Map<TopicPartition, OffsetAndMetadata> prepareOffsetsToReset(...) {
    checkAllTopicPartitionsValid(partitionsToReset);   // throws fast on bad input
    if (opts.options.has(opts.resetToOffsetOpt)) { ... }
    ...
}
```

**How to spot**
CLI tools that accept partition identifiers as input should validate those identifiers before performing any state-mutating or blocking operation. A `describeTopics` call followed by a leader-node check is the standard guard. Grep for `prepareOffsetsToReset` or similar offset-manipulation entry points that lack upfront validation.

**Test**
`testResetOffsetsWithPartitionNoneLeader`: shuts down a broker, calls reset-offsets, asserts `LeaderNotAvailableException` is thrown immediately. `testResetOffsetsWithPartitionNotExist`: requests non-existent partition indices, asserts `UnknownTopicOrPartitionException`.
