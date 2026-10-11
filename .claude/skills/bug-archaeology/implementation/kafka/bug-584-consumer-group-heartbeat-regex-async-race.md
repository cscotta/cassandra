**Commit**: 3f75f450ad · **Ticket**: KAFKA-19481

**Subsystem**: core/src/test/scala/integration/kafka/api/AuthorizerIntegrationTest.scala

**Category**: Race condition/missing synchronization

**Root cause**
`testConsumerGroupHeartbeatWithRegex` failed intermittently because the test asserted on the heartbeat response immediately after sending it, but `GroupMetadataManager.refreshRegularExpressions` is asynchronous and may not have completed before the assertion ran. When `refreshRegularExpressions` had not finished, `memberEpoch` remained at 1 instead of advancing to 2, and `assignment` was null rather than populated. Wrapping the second heartbeat and its assertion in `TestUtils.tryUntilNoAssertionError()` retries until the async operation completes.

**Before**
```scala
// Before: single-shot assertion on async result
val response = sendAndReceiveFirstRegexHeartbeat(...)
sendAndReceiveRegexHeartbeat(response, listenerName, Some(1))
```

**After**
```scala
// After: retry until async regex refresh completes
var response = sendAndReceiveFirstRegexHeartbeat(...)
TestUtils.tryUntilNoAssertionError() {
  response = sendAndReceiveRegexHeartbeat(response, listenerName, Some(1))
}
```

**How to spot**
Any heartbeat-based test that asserts on `memberEpoch` or `assignment` after a regex subscription change without a retry loop is susceptible to this race; `refreshRegularExpressions` is scheduled on a separate thread and its latency is not bounded by the heartbeat round-trip.

**Test**
Add a deliberate 1-second sleep inside `refreshRegularExpressions` and confirm the test still passes with the `tryUntilNoAssertionError` wrapper in place.
