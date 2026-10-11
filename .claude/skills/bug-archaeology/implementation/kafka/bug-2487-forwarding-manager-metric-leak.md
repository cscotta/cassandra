**Commit**: f5439864c6 · **Ticket**: KAFKA-15406
**Subsystem**: core/ForwardingManager
**Category**: Resource leak

**Root cause**
`ForwardingManagerImpl` did not implement `close()`, so the JMX/metrics sensors it registered on startup (QueueTimeMs, RemoteTimeMs, QueueLength from KIP-938) were never unregistered. On broker restart or unit-test teardown, repeated registrations of the same metric name would throw or accumulate stale sensors. Additionally, the KIP-938 metrics themselves were never wired in, so the forwarding path produced no observability. The fix adds `close()` to remove all registered sensors and wires the new metrics into the request-forwarding hot path.

**Before/After**
```java
// Before: no close(), metrics not removed
public class ForwardingManagerImpl implements ForwardingManager {
    // metrics registered in constructor, never removed
}

// After
@Override
public void close() {
    metrics.removeSensor(queueTimeSensor.name());
    metrics.removeSensor(remoteTimeSensor.name());
    metrics.removeSensor(queueLengthSensor.name());
}
```

**How to spot**
Any class that registers metrics sensors in its constructor must implement `AutoCloseable` / `Closeable` and remove those sensors in `close()`. Search for `metrics.sensor(...)` calls in constructors that are not paired with a `removeSensor` in a `close` method.

**Test**
`ForwardingManagerTest` updated to call `close()` and assert that sensors are removed without error.
