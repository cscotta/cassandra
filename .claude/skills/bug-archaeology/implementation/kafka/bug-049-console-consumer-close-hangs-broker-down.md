**Commit**: 64cb839041 · **Ticket**: KAFKA-17853
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerNetworkThread.java (and NetworkClientDelegate)
**Category**: Resource leak

**Root cause**
When a broker disconnected, the heartbeat failure triggered retriable `FindCoordinator` requests that were queued in `NetworkClientDelegate` without an assigned node (`Optional.empty()`). Even after `Ctrl-C` called `ConsumerNetworkThread.close()`, the close path kept polling until all pending requests were sent or timed out, waiting the full default timeout (~30 seconds) because the unresolvable `FindCoordinator` requests could not be dispatched. The fix changes the close-path `poll` call to pass `true` for a `discardPendingUnsentRequests` flag, which removes all unsent requests with no node before polling, so close completes immediately.

**Before/After**
```java
// Before – close path polls without discarding unresolvable pending requests
do {
    networkClientDelegate.poll(timer.remainingMs(), timer.currentTimeMs());
    timer.update();
} while (timer.notExpired() && networkClientDelegate.hasAnyPendingRequests());

// After – discard unresolvable pending requests before polling
do {
    networkClientDelegate.poll(timer.remainingMs(), timer.currentTimeMs(), true);
    timer.update();
} while (timer.notExpired() && networkClientDelegate.hasAnyPendingRequests());
```

**How to spot**
Close/shutdown paths that call `poll` in a retry loop should discard or cancel requests that cannot make progress (no target node) to avoid waiting the full timeout. Look for `poll` calls in teardown paths that pass `false` or no discard flag.

**Test**
`PlaintextConsumerTest#testClassicConsumerCloseOnBrokerShutdown` and `testAsyncConsumerCloseOnBrokerShutdown`; `ShareConsumerTest#testConsumerCloseOnBrokerShutdown`: each shuts down the single broker, does a poll to trigger the disconnect, then calls `consumer.close()` and asserts it completes within 5 seconds using `assertTimeoutPreemptively`.
