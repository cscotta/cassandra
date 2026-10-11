**Commit**: 800de133bf · **Ticket**: KAFKA-17634
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
WakeupTrigger was refactored as a result of changes in AsyncKafkaConsumer. This PR makes the equivalent changes in ShareConsumerImpl. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            return subscriptions.subscription();
            wakeupTrigger.clearTask();
     *     If there is no pending task, set the pending task active.
     *     If wakeup was called before setting an active task, the current task will complete exceptionally with
     *     WakeupException right
     *     away.
     *     if there is an active task, throw exception.
            } else if (task instanceof ActiveFuture || task instanceof FetchAction) {

// After
            return Collections.unmodifiableSet(subscriptions.subscription());
        wakeupTrigger.setShareFetchAction(fetchBuffer);

            wakeupTrigger.clearTask();
            } else if (task instanceof ShareFetchAction) {
                ShareFetchAction shareFetchAction = (ShareFetchAction) task;
                shareFetchAction.fetchBuffer().wakeup();
                return new WakeupFuture();
     *  If there is no pending task, set the pending task active.
     *  If wakeup was called before setting an active task, the current task will complete exceptionally with
     *  WakeupException right away.
     *  If there is an active task, throw exception.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17634 tweak wakeup logic to match wakeuptrigger changes (#17304).

**Test**
Test modified in ShareConsumerImplTest.java covering the corrected behaviour.
