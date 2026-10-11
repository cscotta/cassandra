**Commit**: 676228c708 · **Ticket**: KAFKA-17769
**Subsystem**: clients/consumer (PlaintextConsumerSubscriptionTest)
**Category**: Race condition / missing synchronization

**Root cause**
A test for the new consumer protocol sent a leave-group heartbeat before the broker had assigned a `memberId` to the consumer. Because the consumer sends the leave via the background heartbeat thread, the test's assertion executed while `memberId` was still empty, making the test flaky: sometimes the leave was sent correctly, sometimes it was sent with no member identity and silently ignored.

**Before/After**
```scala
// Before – no guard, races with heartbeat thread
consumer.unsubscribe()
// assert immediately

// After – wait for non-empty memberId first
TestUtils.waitUntilTrue(
  () => consumer.groupMetadata().memberId().nonEmpty,
  "Member ID was not assigned in time")
consumer.unsubscribe()
```

**How to spot**
In consumer protocol integration tests, any assertion that depends on the broker having acknowledged group membership (memberId, generationId, assignment) must be guarded by a `waitUntilTrue` polling the relevant metadata field before acting.

**Test**
Test itself is the fix; flaky test stabilised by the guard.
