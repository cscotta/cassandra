**Commit**: 8f86b9c4ec · **Ticket**: KAFKA-16637
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Allow the committed offsets fetch to run for as long as needed. This handles the case where a user invokes Consumer.poll() with a very small timeout (including zero). Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lianetmr@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            final FetchCommittedOffsetsEvent event =
                new FetchCommittedOffsetsEvent(
                    initializingPartitions,
                    calculateDeadlineMs(timer));
            wakeupTrigger.setActiveTask(event.future());
            final Map<TopicPartition, OffsetAndMetadata> offsets = applicationEventHandler.addAndGet(event);
            log.error("Couldn't refresh committed offsets before timeout expired");

// After
    private FetchCommittedOffsetsEvent pendingOffsetFetchEvent;


        // The shorter the timeout provided to poll(), the more likely the offsets fetch will time out. To handle
        // this case, on the first attempt to fetch the committed offsets, a FetchCommittedOffsetsEvent is created
        // (with potentially a longer timeout) and stored. The event is used for the first attempt, but in the
        // case it times out, subsequent attempts will also use the event in order to wait for the results.
        if (!canReusePendingOffsetFetchEvent(initializingPartitions)) {
            // Give the event a reasonable amount of time to complete.
            final long timeoutMs = Math.max(defaultApiTimeoutMs, timer.remainingMs());
            final long deadlineMs = calculateDeadlineMs(time, timeoutMs);
            pendingOffsetFetchEvent = new FetchCommittedOffsetsEvent(initializingPartitions, deadlineMs);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16637 asynckafkaconsumer removes offset fetch responses from cache too aggressively (#16310).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
