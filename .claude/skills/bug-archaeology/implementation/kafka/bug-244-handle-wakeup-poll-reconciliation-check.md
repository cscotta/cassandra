**Commit**: 7e1c9db92f · **Ticket**: KAFKA-20332
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Minor follow-up to ensure we handle wake-up when checking reconciliation
future in poll. Reviewers: Andrew Schofield <aschofield@confluent.io>, TengYao Chi
 <frankvicky@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
                    wakeupTrigger.setActiveTask(inflightPoll.reconciliationCheckFuture());
                } finally {
                    wakeupTrigger.clearTask();
    @Test
    public void testWakeupWhileWaitingOnReconciliationCheck() {
        FetchBuffer fetchBuffer = mock(FetchBuffer.class);
        SubscriptionState subscriptions = new SubscriptionState(new LogContext(), AutoOffsetResetStrategy.NONE);
        consumer = newConsumer(fetchBuffer, mock(ConsumerInterceptors.class),
            mock(ConsumerRebalanceListenerInvoker.class), subscriptions);

        final TopicPartition tp = new TopicPartition("topic1", 0);
        subscriptions.assignFromUser(singleton(tp));
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20332 [2]: handle wakeup on poll reconciliation check (#21997).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
