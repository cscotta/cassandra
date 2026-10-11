**Commit**: bf760d4ebe · **Ticket**: KAFKA-18558
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                            }
                            sessionHandler.addPartitionToFetch(tip, acknowledgementsToSend);
                            partitionsToForgetMap.putIfAbsent(node, new ArrayList<>());
                            partitionsToForgetMap.get(node).add(tip);
                            topicNamesMap.putIfAbsent(new IdAndPartition(tip.topicId(), tip.partition()), tip.topic());
                            fetchedPartitions.add(tip);
                            log.debug("Added fetch request for previously subscribed partition {} to node {}", tip, node.id());

// After
                                sessionHandler.addPartitionToFetch(tip, acknowledgementsToSend);
                                handlerMap.put(node, sessionHandler);

                                partitionsToForgetMap.putIfAbsent(node, new ArrayList<>());
                                partitionsToForgetMap.get(node).add(tip);
                                topicNamesMap.putIfAbsent(new IdAndPartition(tip.topicId(), tip.partition()), tip.topic());
                                fetchedPartitions.add(tip);
                                log.debug("Added fetch request for previously subscribed partition {} to node {}", tip, node.id());
                            }
import org.apache.kafka.common.requests.ShareFetchRequest;

        client.prepareResponse(fullAcknowledgeResponse(tip1, Errors.NONE));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18558: added check before adding previously subscribed partitions (#18562).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
