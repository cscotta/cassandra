**Commit**: 435e9851d2 · **Ticket**: KAFKA-17505
**Subsystem**: clients/consumer (AsyncKafkaConsumer)
**Category**: Race condition / missing synchronization

**Root cause**
`AsyncKafkaConsumer.seekToBeginning` and `seekToEnd` called `subscriptions.requestOffsetReset()` directly on the calling (application) thread while the background network thread was concurrently reading and acting on the subscription state. This was an unsynchronised cross-thread mutation that could corrupt the fetch-position state. The fix dispatches a new `RESET_OFFSET` event through the single-writer `ApplicationEventHandler`, ensuring the mutation happens on the background thread only.

**Before/After**
```java
// Before – direct mutation from calling thread
public void seekToBeginning(Collection<TopicPartition> partitions) {
    subscriptions.requestOffsetReset(tp, OffsetResetStrategy.EARLIEST);
}

// After – dispatched to background thread
public void seekToBeginning(Collection<TopicPartition> partitions) {
    applicationEventHandler.addAndGet(
        new ResetOffsetEvent(partitions, OffsetResetStrategy.EARLIEST));
}
```

**How to spot**
Any method on the consumer's public API that mutates `SubscriptionState` without going through the event handler is a thread-safety violation; the rule is that only the background I/O thread may write subscription state.

**Test**
Unit tests added in `AsyncKafkaConsumerTest` for `seekToBeginning`/`seekToEnd` event dispatch.
