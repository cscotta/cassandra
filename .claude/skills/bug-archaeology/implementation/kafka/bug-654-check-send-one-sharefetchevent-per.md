**Commit**: c5ae7b4128 · **Ticket**: KAFKA-18794
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
*What*

- Currently in `ShareConsumerImpl`, there is a chance we could be
sending multiple `ShareFetchEvents` for a single `poll()`. - This could happen when the `ShareFetchBuffer` is empty in the first
wait and the `pollTimer` has not completed yet, then we would send
multiple events. - Usually the `ShareFetchBuffer` would wait for a time until the
pollTimeout, so we would not send multiple events, but the buffer wait
time is also controlled by `applicationEventHandler.maximumTimeToWait()`
which can return lower values (even 0) in some cases (especially during
startup of heartbeat request manager). - If this happens, we will see multiple events sent, and this could even
lead to multiple fet...

**Before/After**
```java
// Before
    public void fetch(Map<TopicIdPartition, NodeAcknowledgements> acknowledgementsMap,
                      Map<TopicIdPartition, NodeAcknowledgements> controlRecordAcknowledgements) {
        // Process both acknowledgement maps and sends them in the next ShareFetch.
        processAcknowledgementsMap(controlRecordAcknowledgements);
                // Check for any acknowledgements which could have come from control records (GAP) and include them.
                applicationEventHandler.add(new ShareFetchEvent(acknowledgementsMap, fetch.takeAcknowledgedRecords()));
                // Notify the network thread to wake up and start the next round of fetching
                applicationEventHandler.wakeupNetworkThread();

// After
    public void fetch(Map<TopicIdPartition, NodeAcknowledgements> acknowledgementsMap) {
        // Store the acknowledgements and send them in the next ShareFetch.
    private boolean shouldSendShareFetchEvent = false;
            shouldSendShareFetchEvent = true;

                Map<TopicIdPartition, NodeAcknowledgements> controlRecordAcknowledgements = fetch.takeAcknowledgedRecords();
                if (!controlRecordAcknowledgements.isEmpty()) {
                    // Asynchronously commit any waiting acknowledgements from control records.
                    sendShareAcknowledgeAsyncEvent(controlRecordAcknowledgements);
                }
                // We only send one ShareFetchEvent per poll call.
                if (shouldSendShareFetchEvent) {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18794: add check to send one sharefetchevent per poll in shareconsumer. (#20794).

**Test**
Test modified in KafkaShareConsumerTest.java covering the corrected behaviour.
