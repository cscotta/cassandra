**Commit**: 873379873e · **Ticket**: KAFKA-19435

**Subsystem**: Consumer Group CLI / ConsumerGroupCommand

**Category**: Missing null/bounds check

**Root cause**
`ConsumerGroupCommand.describePartitions` called `admin.listOffsets` for every partition in the group, including partitions with no current leader (`leader == null` / `leaderId == Node.NO_NODE_ID`). Sending `ListOffsets` requests for leaderless partitions results in errors or hangs. The fix filters out partitions without a leader before calling `listOffsets` and displays `-` in the output for their offsets, matching the behavior already used for other unavailable metadata fields.

**Before/After**
```java
// Before: listOffsets called for all partitions including leaderless
Map<TopicPartition, OffsetSpec> offsetRequest = partitions.stream()
    .collect(Collectors.toMap(p -> p.topicPartition(), p -> OffsetSpec.latest()));
admin.listOffsets(offsetRequest).all().get();

// After: leaderless partitions excluded from the request
Map<TopicPartition, OffsetSpec> offsetRequest = partitions.stream()
    .filter(p -> p.leader() != null && p.leader().id() != Node.NO_NODE_ID)
    .collect(Collectors.toMap(p -> p.topicPartition(), p -> OffsetSpec.latest()));
admin.listOffsets(offsetRequest).all().get();
```

**How to spot**
Any code that sends per-partition admin requests (ListOffsets, DescribeLogDirs) without first checking that the partition has a valid leader will fail when partitions are under-replicated or in leader election. Always filter on leader availability before constructing such request maps.

**Test**
`ConsumerGroupCommandTest` adds a scenario with a leaderless partition and verifies that `listOffsets` is not called for it and that `-` is shown in the describe output.
