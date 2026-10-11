**Commit**: 2b495945a2 · **Ticket**: KAFKA-17377
**Subsystem**: clients/consumer/internals/AsyncKafkaConsumer
**Category**: Wrong constant / default value

**Root cause**
`AsyncKafkaConsumer.unsubscribe()` set the deadline for the `UnsubscribeEvent` to `Long.MAX_VALUE`, effectively making the unsubscribe call wait forever for the background thread to process it. All other consumer operations (assign, subscribe, commitSync) used `defaultApiTimeoutMs` as their deadline. This inconsistency meant that `unsubscribe()` could hang indefinitely if the background thread was stuck, rather than failing with a `TimeoutException` after the configured API timeout.

**Before/After**
```java
// Before
Timer timer = time.timer(Long.MAX_VALUE);  // infinite wait
UnsubscribeEvent unsubscribeEvent = new UnsubscribeEvent(calculateDeadlineMs(timer));

// After
Timer timer = time.timer(defaultApiTimeoutMs);  // respects configured timeout
UnsubscribeEvent unsubscribeEvent = new UnsubscribeEvent(calculateDeadlineMs(timer));
```

**How to spot**
In async consumer methods that dispatch events to the background thread, verify that every timer or deadline is derived from `defaultApiTimeoutMs` (or another appropriate timeout config), not from a hard-coded sentinel like `Long.MAX_VALUE`. A `Long.MAX_VALUE` timer in a blocking `addAndGet` call is equivalent to an indefinite block.

**Test**
Test updated to assert the deadline is set to `defaultApiTimeoutMs`.
