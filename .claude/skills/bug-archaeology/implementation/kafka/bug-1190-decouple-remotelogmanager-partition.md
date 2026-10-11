**Commit**: 321a380d0a · **Ticket**: KAFKA-14523
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Remove the last dependency in the core module. Reviewers: Luke Chen <showuon@gmail.com>, PoAn Yang <poan.yang@suse.com>

**Before/After**
```java
// Before
import kafka.cluster.Partition;

    private Stream<Partition> filterPartitions(Set<Partition> partitions) {
        return partitions.stream().filter(partition -> partition.log().exists(UnifiedLog::remoteLogEnabled));
    public void onLeadershipChange(Set<Partition> partitionsBecomeLeader,
                                   Set<Partition> partitionsBecomeFollower,
                .collect(Collectors.toMap(p -> new TopicIdPartition(topicIds.get(p.topic()), p.topicPartition()),
                        p -> p.log().exists(log -> log.config().remoteLogCopyDisable())));
                .collect(Collectors.toMap(p -> new TopicIdPartition(topicIds.get(p.topic()), p.topicPartition()),
                        p -> p.log().exists(log -> log.config().remoteLogCopyDisable())));
    public void stopLeaderCopyRLMTasks(Set<Partition> partitions) {
        for (Partition partition : partitions) {

// After
import org.apache.kafka.server.log.remote.TopicPartitionLog;
    private Stream<TopicPartitionLog> filterPartitions(Set<TopicPartitionLog> partitions) {
        return partitions.stream().filter(partition -> partition.unifiedLog().isPresent() && partition.unifiedLog().get().remoteLogEnabled());
    public void onLeadershipChange(Set<TopicPartitionLog> partitionsBecomeLeader,
                                   Set<TopicPartitionLog> partitionsBecomeFollower,
                .collect(Collectors.toMap(p -> new TopicIdPartition(topicIds.get(p.topicPartition().topic()), p.topicPartition()),
                        p -> p.unifiedLog().isPresent() ? p.unifiedLog().get().config().remoteLogCopyDisable() : false));
                .collect(Collectors.toMap(p -> new TopicIdPartition(topicIds.get(p.topicPartition().topic()), p.topicPartition()),
                        p -> p.unifiedLog().isPresent() ? p.unifiedLog().get().config().remoteLogCopyDisable() : false));
    public void stopLeaderCopyRLMTasks(Set<TopicPartitionLog> partitions) {
        for (TopicPartitionLog partition : partitions) {
import org.apache.kafka.server.log.remote.TopicPartitionLog
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14523: decouple remotelogmanager and partition (#19391).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
