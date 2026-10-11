# Bug 0044: Accord Topology Initialization Waits Indefinitely on Stuck Epoch

**Commit**: 64934507ae · **Ticket**: CASSANDRA-20716
**Subsystem**: service/accord/AccordService
**Category**: Logic error in condition

---

## Root Cause

The `while(true)` loop that waits for Accord's topology epoch to become ready had no upper time bound. If the configuration service could never satisfy the awaited epoch (e.g. due to a missing topology delivery, a network partition during startup, or any other bug in topology propagation), the node would spin-wait forever, logging warnings every five seconds without any way to break out or surface diagnostic information. The fix introduces a 60-second deadline using `Clock.Global.nanoTime()` and throws an `IllegalStateException` that includes a dump of the configuration service's internal state for diagnostics.

### Before / After

```java
// BEFORE — unbounded loop, hangs forever
int attempt = 0;
int waitSeconds = 5;
while (true) {
    ...
    logger.warn("Epoch {} is not ready after waiting for {} seconds", ...);
}
```

```java
// AFTER — 60-second deadline with diagnostic exception
int attempt = 0;
int waitSeconds = 5;
long deadine = Clock.Global.nanoTime() + SECONDS.toNanos(60);
while (true) {
    ...
    logger.warn("Epoch {} is not ready after waiting for {} seconds", ...);
    if (Clock.Global.nanoTime() > deadine)
        throw new IllegalStateException(String.format(
            "Could not initialize epoch %s. Config service state:\n%s",
            await, configService.getDebugStr()));
}
```

---

## How to Spot

Every startup `while(true)` loop that waits for an external condition should have an explicit deadline or attempt limit, along with a diagnostic exception or forced-shutdown on expiry. Review any retry loop where only a warning is logged on timeout — if there is no escape hatch, the node can hang indefinitely on any upstream failure.

---

## Test

No test added; the fix is a defensive safeguard against an infinite hang.
