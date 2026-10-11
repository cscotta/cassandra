**Commit**: 6b2013a001 · **Ticket**: KAFKA-19294
**Subsystem**: core/src/main/scala/kafka/server/BrokerLifecycleManager.scala
**Category**: Wrong constant / default value

**Root cause**
`BrokerLifecycleManager` used an `ExponentialBackoff` with a maximum delay of `brokerSessionTimeoutMs / 2` after failed heartbeat/registration RPCs. Additionally, the maximum RPC timeout for the `NodeToControllerChannelManager` was set to `brokerSessionTimeoutMs / 2`. In the worst case (RPC timeout followed immediately by backoff), the broker could spend a full `brokerSessionTimeoutMs` without sending a heartbeat, causing the controller to consider it dead and triggering unnecessary leader elections. The fix removes exponential backoff for heartbeats (the load from heartbeats is low) and sets the maximum RPC time to `brokerHeartbeatIntervalMs` so the combined delay never exceeds one heartbeat interval.

**Before/After**
```scala
// Before: exponential backoff up to sessionTimeoutMs/2; RPC timeout also sessionTimeoutMs/2
private val resendExponentialBackoff =
    new ExponentialBackoff(100, 2, config.brokerSessionTimeoutMs.toLong / 2, 0.02)

private def scheduleNextCommunicationAfterFailure(): Unit = {
    val delayMs = resendExponentialBackoff.backoff(failedAttempts)
    failedAttempts += 1
    scheduleNextCommunication(NANOSECONDS.convert(delayMs, MILLISECONDS))
}

// After: fixed delay of one heartbeat interval; no exponential backoff
private def scheduleNextCommunicationAfterFailure(): Unit = {
    scheduleNextCommunication(NANOSECONDS.convert(
        config.brokerHeartbeatIntervalMs.longValue(), MILLISECONDS))
}
```

**How to spot**
For heartbeat mechanisms, the retry delay after failure must be bounded by the heartbeat interval (or a small multiple of it) rather than the session timeout. Any backoff whose maximum exceeds the heartbeat interval can cause the controller to declare the broker dead. Verify that `scheduleNextCommunicationAfterFailure` delays are at most one heartbeat interval.

**Test**
No new test was added; this is a correctness fix validated by the described failure scenario where brokers missed heartbeats under high RPC load.
