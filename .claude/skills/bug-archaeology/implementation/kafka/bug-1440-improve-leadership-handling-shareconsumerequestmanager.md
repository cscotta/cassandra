**Commit**: 0e40b80c86 · **Ticket**: KAFKA-18769
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        // which are no longer part of the current subscription, or whose records were fetched from a
        // previous leader.
                            metricsManager.recordAcknowledgementSent(acks.size());
                            fetchAcknowledgementsInFlight.computeIfAbsent(node.id(), k -> new HashMap<>()).put(tip, acks);
                            sessionHandler.addPartitionToAcknowledgeOnly(tip, acks);
                            handlerMap.put(node, sessionHandler);
                            topicNamesMap.putIfAbsent(new IdAndPartition(tip.topicId(), tip.partition()), tip.topic());
                            log.debug("Added fetch request for previously subscribed partition {} to node {}", tip, nodeId);
                        acknowledgementsMapForNode.put(tip, nodeAcknowledgements.acknowledgements());
                        metricsManager.recordAcknowledgementSent(nodeAcknowledgements.acknowledgements().size());
                        log.debug("Added sync acknowledge request for partition {} to node {}", tip.topicPartition(), node.id());
                        resultCount.incrementAndGet();

// After
        // which are no longer part of the current subscription.
        // We fail acknowledgements for records fetched from a previous leader.
                            if (!isLeaderKnownToHaveChanged(nodeId, tip)) {
                                metricsManager.recordAcknowledgementSent(acks.size());
                                fetchAcknowledgementsInFlight.computeIfAbsent(node.id(), k -> new HashMap<>()).put(tip, acks);
                                sessionHandler.addPartitionToAcknowledgeOnly(tip, acks);
                                handlerMap.put(node, sessionHandler);
                                topicNamesMap.putIfAbsent(new IdAndPartition(tip.topicId(), tip.partition()), tip.topic());
                                log.debug("Added fetch request for previously subscribed partition {} to node {}", tip, nodeId);
                            } else {
                                log.debug("Leader for the partition is down or has changed, failing Acknowledgements for partition {}", tip);
                                acks.complete(Errors.NOT_LEADER_OR_FOLLOWER.exception());
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18769: improve leadership changes handling in shareconsumerequestmanager. (#18851).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
