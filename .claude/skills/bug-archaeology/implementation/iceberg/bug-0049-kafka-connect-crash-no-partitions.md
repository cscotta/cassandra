# Bug 0049: Kafka Connect coordinator crashes with ConnectException when no partitions are assigned

**Commit**: a675f88b0d8b1bc5b9f37bc8db304bd694cf5f18 · **Ticket**: #15955

## Detection Rule

When using `Stream.min()` or `Stream.max()` to elect a leader or pick a representative element, verify that the `.orElseThrow()` terminal is replaced with `.orElse(null)` (or similar) and the null case is handled gracefully, because the stream may legitimately be empty during transient rebalances.

## Code Smell

`stream().min(...).orElseThrow(() -> new ConnectException("No partitions assigned"))` inside a method that determines cluster leadership; the exception propagates instead of gracefully yielding `false` (not the leader).

## Tags

- scope: distributed-systems
- category: missing-null-check
- code-pattern: stream-min-throws-on-empty
- severity: crash
- subsystem: kafka-connect/channel/CommitterImpl

## Root Cause

`containsFirstPartition()` found the globally-first partition by streaming all members' assignments and calling `.orElseThrow()`. During a Kafka rebalance it is valid for all members to temporarily have no assigned partitions; in that window the stream is empty and `orElseThrow` threw a `ConnectException`, crashing the connector task. The fix returns `null` from a helper and treats a null first-partition as "not the leader" with a warning log.

## Example

```java
// BEFORE
boolean containsFirstPartition(Collection<MemberDescription> members,
                               Collection<TopicPartition> partitions) {
    TopicPartition firstTopicPartition = members.stream()
        .flatMap(m -> m.assignment().topicPartitions().stream())
        .min(new TopicPartitionComparator())
        .orElseThrow(() -> new ConnectException("No partitions assigned, cannot determine leader"));
    return partitions.contains(firstTopicPartition);
}
```

```java
// AFTER
boolean containsFirstPartition(Collection<MemberDescription> members,
                               Collection<TopicPartition> partitions) {
    TopicPartition firstTopicPartition = findFirstTopicPartition(members);
    if (firstTopicPartition == null) {
        LOG.warn("No partitions assigned across all members, cannot determine leader");
        return false;
    }
    return partitions.contains(firstTopicPartition);
}

TopicPartition findFirstTopicPartition(Collection<MemberDescription> members) {
    return members.stream()
        .flatMap(m -> m.assignment().topicPartitions().stream())
        .min(new TopicPartitionComparator())
        .orElse(null);
}
```
