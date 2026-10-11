**Commit**: a696b4d6f4 · **Ticket**: KAFKA-17978
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
When we introduced "startup tasks" in #16922, we initialized them with
no input partitions, because they aren't known until assignment. However, when we update them during assignment, it's possible that we
update the topology with the incorrect source topics for some internal
topics, due to a difference in the way internal topics are handled for
StandbyTasks. To resolve this, we now initialize startup tasks with the correct input
partitions, by calculating them from the Topology. When we assign our startup tasks, we now conditionally update their
input partitions only if they've actually changed, just as we do for
regular StandbyTasks.

**Before/After**
```java
// Before
import java.util.HashSet;
        return new ProcessorStateManager(taskId, TaskType.STANDBY, eosEnabled, logContext, stateDirectory, null, storeToChangelogTopic, new HashSet<>(0), stateUpdaterEnabled);
                        new HashSet<>(),
                    task.updateInputPartitions(inputPartitions, topologyMetadata.nodeToSourc

// After
                                                               final Set<TopicPartition> sourcePartitions,
        return new ProcessorStateManager(taskId, TaskType.STANDBY, eosEnabled, logContext, stateDirectory, null, storeToChangelogTopic, sourcePartitions, stateUpdaterEnabled);
import org.apache.kafka.common.TopicPartition;
import java.util.Collection;
                    final Set<TopicPartition> inputPartitions = topologyMetadata.nodeToSourceTopics(id).values().stream()
                            .flatMap(Collection::stream)
                            .map(t -> new TopicPartition(t, id.partition()))
                            .collect(Collectors.toSet());
                        inputPartitions,
                        inputPartitions,
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17978: fix invalid topology on task assignment (#17778).

**Test**
No test added.
