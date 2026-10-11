**Commit**: a8ab55af74 · **Ticket**: KAFKA-17731
**Subsystem**: clients (ClientTelemetryReporter, KafkaProducer, AsyncKafkaConsumer)
**Category**: Deadlock

**Root cause**
`ClientTelemetryReporter.initiateClose(long timeoutMs)` held an internal lock while calling `await()` on a condition variable that was signalled by the network I/O thread. If the network thread was blocked waiting for the same lock (e.g., during a `send` call), a deadlock resulted. Additionally, passing a timeout to `initiateClose` was redundant: the caller already controlled the overall close timeout. Removing the `await()` and the `terminalPushInProgress` condition variable eliminated the deadlock path entirely.

**Before/After**
```java
// Before – await() inside locked section, potential deadlock
synchronized (this) {
    terminalPushInProgress = true;
    while (!telemetryCompleted)
        this.wait(timeoutMs); // deadlock if net thread blocked
}

// After – fire-and-forget, network thread drains asynchronously
void initiateClose() {
    setState(ClientTelemetryState.TERMINATING_PUSH_NEEDED);
}
```

**How to spot**
Any `condition.await()` or `Object.wait()` call inside a `synchronized` block that is also entered by I/O threads is a deadlock candidate; prefer state-machine transitions that let background threads drain without blocking the caller.

**Test**
No test added.
