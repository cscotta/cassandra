**Commit**: 47bb46c10d · **Ticket**: KAFKA-19582
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
The ReassignPartitionsCommand shows the topic replicas on each broker. When using the --generate command, it returns the current partition
replica assignment. However, the log directory for each current replica
is always shown as any. This makes it impossible for users to determine
which specific log directory is being used by each replica.

**Before/After**
```java
// Before
            formatAsReassignmentJson(currentAssignments, Map.of()));
        System.out.println(currentPartitionReplicaAssignmentToString(proposedParts, currentParts));
    static String currentPartitionReplicaAssignmentToString(Map<TopicPartition, List<Integer>> proposedParts,
                                                            Map<TopicPartition, List<Integer>> currentParts) throws JsonProcessingException {
            formatAsReassignmentJson(partitionsToBeReassigned, Map.of()),

// After
        Map<TopicPartitionReplica, String> currentReplicaLogDirs = getReplicaToLogDir(adminClient, currentAssignments);
            formatAsReassignmentJson(currentAssignments, currentReplicaLogDirs));
        System.out.println(currentPartitionReplicaAssignmentToString(adminClient, proposedParts, currentParts));
     * @param adminClient                 The admin client object to use.
    static String currentPartitionReplicaAssignmentToString(Admin adminClient,
                                                            Map<TopicPartition, List<Integer>> proposedParts,
                                                            Map<TopicPartition, List<Integer>> currentParts) throws JsonProcessingException, ExecutionException, InterruptedException {
        Map<TopicPartitionReplica, String> currentReplicaLogDirs = getReplicaToLogDir(adminClient, partitionsToBeReassigned);
            formatAsReassignmentJson(partitionsToBeReassigned, currentReplicaLogDirs),

    static Map<TopicPartitionReplica, String> getReplicaToLogDir(
        Admin adminClient,
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19582 the current assignments shown by reassignpartitionscommand should include the log directories (#20319).

**Test**
Test modified in ReassignPartitionsUnitTest.java covering the corrected behaviour.
