**Commit**: b6b2c9ebc4 · **Ticket**: KAFKA-16985
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Lianet Magrans <lmagrans@confluent.io>, Philip Nee <pnee@confluent.io>

**Before/After**
```java
// Before
import java.util.Collection;
     * member fails ({@link #transitionToFatal()} or leaves the group ({@link #leaveGroup()}).
     * If the member is currently leaving the group after a call to {@link #leaveGroup()}}, this
     * will have a future that will complete when the ongoing leave operation completes
     * (callbacks executed and heartbeat request to leave is sent out). This will be empty is the
        Collection<TopicPartition> assignedTopicPartitions = toTopicPartitionSet(assignedPartitions);
     * This is expected to be invoked when the user calls the unsubscribe API.

// After
     * member fails ({@link #transitionToFatal()} or leaves the group
     * ({@link #leaveGroup()}/{@link #leaveGroupOnClose()}).
     * If the member is currently leaving the group after a call to {@link #leaveGroup()} or
     * {@link #leaveGroupOnClose()}, this will have a future that will complete when the ongoing leave operation
     * completes (callbacks executed and heartbeat request to leave is sent out). This will be empty is the
            notifyAssignmentChange(Collections.emptySet());
        Set<TopicPartition> assignedTopicPartitions = toTopicPartitionSet(assignedPartitions);
        notifyAssignmentChange(assignedTopicPartitions);
     * This is expected to be invoked when the user calls the {@link Consumer#close()} API.
     *
     * @return Future that will complete when the heartbeat to leave the group has been sent out.
     */
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16985: ensure consumer attempts to send leave request on close even if interrupted (#16686).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
