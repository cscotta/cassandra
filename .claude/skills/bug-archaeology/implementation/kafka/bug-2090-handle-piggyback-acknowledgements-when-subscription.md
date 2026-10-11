**Commit**: fd4d58899b · **Ticket**: KAFKA-17819
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Currently in ShareConsumeRequestManager, after we receive a ShareFetchResponse, if the subscription changes before we acknowledge(via ShareFetch), then we do not acknowledge the records which are not part of the updated subscription. Instead we must acknowledge all the records that we had received irrespective of the current subscription. This bug is only when we are acknowledging via ShareFetch where we use SubscriptionState::fetchablePartitions to obtain the partitions to fetch. In ShareAcknowledge, as we are getting the partitions from the active share sessions, even if the subscription changed, the session would remain active.

**Before/After**
```java
// Before
                log.debug("Added fetch request for partition {} to node {}", partition, node.id());
            builderMap.put(entry.getKey(), entry.getValue().newShareFetchBuilder(groupId, fetchConfig));

// After
import java.util.Objects;
    private final Map<IdAndPartition, String> forgottenTopicNames = new HashMap<>();
        Set<TopicIdPartition> fetchedPartitions = new HashSet<>();
                fetchedPartitions.add(tip);
                log.debug("Added fetch request for partition {} to node {}", tip, node.id());
        // Map storing the list of partitions to forget in the upcoming request.
        Map<Node, List<TopicIdPartition>> partitionsToForgetMap = new HashMap<>();
        Cluster cluster = metadata.fetch();
        // Iterating over the session handlers to see if there are acknowledgements to be sent for partitions
        // which are no longer part of the current subscription.
        sessionHandlers.forEach((nodeId, sessionHandler) -> {
            Node node = cluster.nodeById(nodeId);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17819 : handle piggyback acknowledgements when subscription changes in shareconsumerequestmanager. (#17537).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
