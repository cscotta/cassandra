**Commit**: 06ce3e890b · **Ticket**: KAFKA-18733
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                    acknowledgePartitionBatches.forEach(batch -> batch.acknowledgeTypes().forEach(shareGroupMetrics::recordAcknowledgement));
        assertEquals(2, shareGroupMetrics.recordAcknowledgementMeter(AcknowledgeType.ACCEPT.id).count());
        assertEquals(2, shareGroupMetrics.recordAcknowledgementMeter(AcknowledgeType.RELEASE.id).count());
        assertEquals(2, shareGroupMetrics.recordAcknowledgementMeter(AcknowledgeType.REJECT.id).count());

// After
                    acknowledgePartitionBatches.forEach(batch -> {
                        // Client can either send a single entry in acknowledgeTypes which represents
                        // the state of the complete batch or can send individual offsets state.
                        if (batch.acknowledgeTypes().size() == 1) {
                            shareGroupMetrics.recordAcknowledgement(batch.acknowledgeTypes().get(0), batch.lastOffset() - batch.firstOffset() + 1);
                        } else {
                            batch.acknowledgeTypes().forEach(shareGroupMetrics::recordAcknowledgement);
                        }
                    });
        assertEquals(42, shareGroupMetrics.recordAcknowledgementMeter(AcknowledgeType.ACCEPT.id).count());
        assertEquals(35, shareGroupMetrics.recordAcknowledgementMeter(AcknowledgeType.RELEASE.id).count());
        assertEquals(18, shareGroupMetrics.recordAcknowledgementMeter(AcknowledgeType.REJECT.id).count());
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18733: updating share group record acks metric (2/n) (#18924).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
