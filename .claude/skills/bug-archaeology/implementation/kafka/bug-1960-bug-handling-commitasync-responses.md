**Commit**: 866d66229d · **Ticket**: KAFKA-18056
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
There was a bug in handling the ShareAcknowledgeResponse for commitAsync(). Currently after we receive a response, we send out a background event to the application thread to update the acknowledgement commit callbacks for EVERY TopicIdPartition. The map that was sent was not cleared after sending the event. This meant we ended up sending responses for partitions that were already sent in the previous event.

**Before/After**
```java
// Before
            if (isCommitAsync || (remainingResults  != null && remainingResults.decrementAndGet() == 0)) {
        partitions.forEach(partition -> {
            subscriptions.subscribeToShareGroup(Collections.singleton(partition.topic()));
            subscriptions.assignFromSubscribed(Collections.singleton(partition));
        });

// After
            // For commitAsync, we send out a background event for every TopicIdPartition, so we use a singletonMap each time.
            if (isCommitAsync) {
                maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(partition, acknowledgements));
            } else if (remainingResults != null && remainingResults.decrementAndGet() == 0) {
import java.util.stream.Collectors;
        subscriptions.subscribeToShareGroup(partitions.stream().map(TopicPartition::topic).collect(Collectors.toSet()));
        subscriptions.assignFromSubscribed(partitions);
    @Test
    public void testAcknowledgementCommitCallbackMultiplePartitionCommitAsync() {
        buildRequestManager();
        shareConsumeRequestManager.setAcknowledgementCommitCallbackRegistered(true);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18056: fixed bug in handling commitasync responses (#17909).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
