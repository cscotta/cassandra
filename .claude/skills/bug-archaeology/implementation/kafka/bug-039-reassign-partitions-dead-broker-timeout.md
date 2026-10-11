**Commit**: 04da4fb4e5 · **Ticket**: KAFKA-20023
**Subsystem**: tools/src/main/java/org/apache/kafka/tools/reassign/ReassignPartitionsCommand.java
**Category**: Missing null / bounds check

**Root cause**
`ReassignPartitionsCommand` extracted broker IDs from the replica list with `info.replicas().stream().map(Node::id)` before querying log-directory information. When a broker in the replica list was down, `AdminClient.describeTopics` returned placeholder `Node` objects with `isEmpty() == true` (id=-1). Passing these to `describeReplicaLogDirs` caused a timeout or error because the admin client tried to contact an unknown broker. The fix retains full `Node` objects through the pipeline and filters out empty nodes before calling `describeReplicaLogDirs`.

**Before/After**
```java
// Before – immediately converts to int IDs, losing isEmpty() info
info.replicas().stream().map(Node::id).collect(Collectors.toList())

// After – retain Node objects; filter empties before log-dir query
info.replicas()  // returns List<Node>
// ...
private static List<TopicPartitionReplica> available(Map<TopicPartition, List<Node>> current) {
    return ... .filter(node -> !node.isEmpty()) ...
}
```

**How to spot**
Any code that calls `Node::id` on nodes returned from topic description without first checking `node.isEmpty()` will pass `-1` to broker-targeted APIs. Look for `describeReplicaLogDirs` (or similar) calls where the replica list originates from `TopicPartitionInfo.replicas()`.

**Test**
`ReassignPartitionsCommandTest#testGenerateAssignmentWithOneBootstrapServerShutdownWontTimeout` and `testExecuteAssignmentWithOneBootstrapServerShutdownWontTimeout`: shut down one broker, then invoke `--generate` and `--execute`; assert completion without timeout.
