# Bug 0388: Internode Message Resource Limit Goes Negative on Serialization Failure

**Commit**: 6fa5300682 · **Ticket**: CASSANDRA-16616
**Subsystem**: src/java/org/apache/cassandra/net
**Category**: Logic error in condition

---

## Root Cause

`ResourceLimits.Concurrent.release()` contained an `assert` that the running total (`using`) never went negative after subtracting the released amount. When a message serialization failure occurred, `onFailedSerialize()` called `releaseCapacity()` before logging the error and before calling `JVMStabilityInspector.inspectThrowable()`. This ordering meant the assert fired (in assertion-enabled builds) or silently corrupted the counter before the JVM stability handler had a chance to act. The fix reorders the logging/inspection to occur first, converts the assert to a proper `UnrecoverableIllegalStateException` that is fed through `JVMStabilityInspector` so the JVM shuts down cleanly rather than limping on with an invalid shared limit counter.

### Before / After

```java
// BEFORE – releaseCapacity first, assert after, then log
private void onFailedSerialize(Message<?> message, int messagingVersion, int bytesWrittenToNetwork, Throwable t)
{
    JVMStabilityInspector.inspectThrowable(t);
    releaseCapacity(1, canonicalSize(message));
    ...
    logger.warn("...", t);
    callbacks.onFailedSerialize(...);
}

// In Concurrent.release():
long using = usingUpdater.addAndGet(this, -amount);
assert using >= 0;
```

```java
// AFTER – log first, release second; assert replaced by exception that kills JVM
private void onFailedSerialize(...)
{
    logger.warn("...", t);   // log before any state mutation
    JVMStabilityInspector.inspectThrowable(t);
    releaseCapacity(1, canonicalSize(message));
    ...
}

// In Concurrent.release():
if (using < 0L)
    throw new UnrecoverableIllegalStateException(
        "Internode messaging byte limits ... is invalid (using="+using+")");
```

---

## How to Spot

In any shared-counter accounting path, look for `assert counter >= 0` guarding a value that could go negative due to a double-release or an incorrect size argument from an error path. Such asserts are silent in production (where assertions are disabled) and explosive in tests; they should be replaced by an exception that is routed through the existing stability/kill mechanism. Also check that logging occurs before, not after, state mutation so the log entry is not lost if the mutation throws.

---

## Test

`negativeConcurrentUsingValueKillsJVMTest` in `ResourceLimitsTest` releases more capacity than was ever acquired and asserts that `JVMStabilityInspector` triggered a JVM kill.
