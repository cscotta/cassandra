**Commit**: 4a2577b801 · **Ticket**: KAFKA-17395
**Subsystem**: clients/consumer/KafkaConsumerTest
**Category**: Race condition / missing synchronization

**Root cause**
The test `testMissingOffsetNoResetPolicy` called `MockClient.prepareResponseFrom` for `FindCoordinatorResponse` and `OffsetFetchResponse` *after* creating and assigning to the consumer. In `AsyncKafkaConsumer`, these requests are sent by the background thread immediately upon assignment, so the mock response registrations arrived too late and the background thread found no matching prepared response, causing sporadic `NullPointerException` or wrong assertion failures. The fix is to pre-register mock responses before starting the consumer so the background thread always finds them.

**Before/After**
```java
// Before (responses registered after consumer construction)
consumer = newConsumer(...);
consumer.assign(singletonList(tp0));
client.prepareResponseFrom(FindCoordinatorResponse...);  // too late
client.prepareResponseFrom(offsetResponse(...));          // too late

// After (responses registered before consumer construction)
client.prepareResponseFrom(FindCoordinatorResponse...);
client.prepareResponseFrom(offsetResponse(...));
consumer = newConsumer(...);
consumer.assign(singletonList(tp0));
```

**How to spot**
In async consumer tests, any `MockClient.prepareResponseFrom` call that appears *after* `consumer.assign()` or `consumer.subscribe()` is a candidate for a race with the background network thread. Move all mock setup before the consumer is started or assigned to avoid timing-dependent test failures.

**Test**
Test rearranged to fix the flaky test.
