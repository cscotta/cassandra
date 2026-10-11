**Commit**: a6a588fbed · **Ticket**: KAFKA-18198
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import java.util.Collections;
                        metricsManager.recordAcknowledgementSent(acknowledgementsToSend.size());
                        fetchAcknowledgementsInFlight.computeIfAbsent(node.id(), k -> new HashMap<>()).put(tip, acknowledgementsToSend);
                                maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(tip, acks));
                            maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(tip, nodeAcknowledgements.acknowledgements()));
                            maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(tip, nodeAcknowledgements.acknowledgements()));
                maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(tip, nodeAcks.acknowledgements()));
                            maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(tip, acks));
                        Map<TopicIdPartition, Acknowledgements> acksMap = Collections.singletonMap(tip, acks);

// After
                        if (handler.isNewSession()) {
                            // Failing the acknowledgements as we cannot have piggybacked acknowledgements in the initial ShareFetchRequest.
                            acknowledgementsToSend.complete(Errors.INVALID_SHARE_SESSION_EPOCH.exception());
                            maybeSendShareAcknowledgeCommitCallbackEvent(Map.of(tip, acknowledgementsToSend));
                        } else {
                            metricsManager.recordAcknowledgementSent(acknowledgementsToSend.size());
                            fetchAcknowledgementsInFlight.computeIfAbsent(node.id(), k -> new HashMap<>()).put(tip, acknowledgementsToSend);
                        }
                                maybeSendShareAcknowledgeCommitCallbackEvent(Map.of(tip, acks));
                            maybeSendShareAcknowledgeCommitCallbackEvent(Map.of(tip, nodeAcknowledgements.acknowledgements()));
                            maybeSendShareAcknowledgeCommitCallbackEvent(Map.of(tip, nodeAcknowledgements.acknowledgements()));
                maybeSendShareAcknowledgeCommitCallbackEvent(Map.of(tip, nodeAcks.acknowledgements()));
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18198: added check to prevent acknowledgements on initial sharefetchrequest. (#18944).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
