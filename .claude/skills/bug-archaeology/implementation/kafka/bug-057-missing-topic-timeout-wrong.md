**Commit**: 2baad13cf1 · **Ticket**: KAFKA-19521
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/StreamsRebalanceData.java
**Category**: Wrong constant / default value

**Root cause**
`StreamsGroupHeartbeatRequestManager` used `max.poll.interval.ms` (default 300,000 ms) as the timeout before raising `MissingSourceTopicException`. The actual poll frequency when source topics are absent is `poll.ms` (~100 ms), not the max-poll interval. This mismatch caused integration tests to wait the full 300 seconds instead of a few seconds, and in practice made the exception appear far later than desirable. The broker sends the real heartbeat interval via KIP-1071 responses; the fix stores that value in `StreamsRebalanceData` and uses `2 * heartbeatIntervalMs` as the timeout, falling back to `max.poll.interval.ms` if no heartbeat response has been received yet.

**Before/After**
```java
// Before: missing-topic timeout used max.poll.interval.ms (300 s default)
// (in StreamThread.handleMissingSourceTopicsWithTimeout, effectively no heartbeat-aware timeout)

// After: heartbeatIntervalMs tracked and used
// StreamsRebalanceData gains:
public void setHeartbeatIntervalMs(final int heartbeatIntervalMs) {
    this.heartbeatIntervalMs.set(heartbeatIntervalMs);
}
// and the timeout becomes 2 * heartbeatIntervalMs (or max.poll.interval.ms as fallback)
```

**How to spot**
When a timeout is derived from a configuration value that belongs to a different subsystem (polling vs. heartbeating), cross-check the semantics. Any timeout that should be proportional to heartbeat frequency must either read the heartbeat config or track the broker-reported interval. Look for places where `max.poll.interval.ms` is used outside of poll-loop logic.

**Test**
`testStreamsRebalanceDataHeartbeatIntervalMsUpdatedOnSuccess` verifies that `streamsRebalanceData.heartbeatIntervalMs()` starts as -1, then equals `RECEIVED_HEARTBEAT_INTERVAL_MS` after a successful heartbeat response is processed.
