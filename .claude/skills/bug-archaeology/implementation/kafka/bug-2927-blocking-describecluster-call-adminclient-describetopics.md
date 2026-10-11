**Commit**: c01279b92a · **Ticket**: KAFKA-16905
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lianetmr@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.ExecutionException;
    Call generateDescribeTopicsCallWithMetadataApi(
    Call generateDescribeTopicsCallWithDescribeTopicPartitionsApi(
            return new HashMap<>(topicFutures);
        Map<Integer, Node> nodes;
        try {
            nodes = clusterResult.nodes().get().stream().collect(Collectors.toMap(Node::id, node -> node));
        } catch (InterruptedException | ExecutionException e) {
            completeAllExceptionally(topicFutures.values(), e.getCause());
            return new HashMap<>(topicFutures);
        }


// After
    private Call generateDescribeTopicsCallWithMetadataApi(
    private Call generateDescribeTopicsCallWithDescribeTopicPartitionsApi(
            return Collections.unmodifiableMap(topicFutures);
        clusterResult.nodes().whenComplete(
            (nodes, exception) -> {
                if (exception != null) {
                    completeAllExceptionally(topicFutures.values(), exception.getCause());
                    return;
                }
                final long now = time.milliseconds();
                Map<Integer, Node> nodeIdMap = nodes.stream().collect(Collectors.toMap(Node::id, node -> node));
                runnable.call(
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16905: fix blocking describecluster call in adminclient describetopics (#16217).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
