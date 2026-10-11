**Commit**: 7e405ccc65 · **Ticket**: KAFKA-18758
**Subsystem**: core/src/main/scala/kafka/server/BrokerServer.scala
**Category**: Missing null / bounds check

**Root cause**: `BrokerServer.shutdown()` unconditionally called `lifecycleManager.beginControlledShutdown()`, `lifecycleManager.beginShutdown()`, and `lifecycleManager.close()`. If the broker startup sequence threw an `InvalidConfigurationException` (or any other exception) before `lifecycleManager` was initialised, the shutdown path would throw `NullPointerException`, obscuring the original error. Similarly, `logManager.shutdown(lifecycleManager.brokerEpoch)` would NPE if `lifecycleManager` was null. The fix adds `if (lifecycleManager != null)` guards around each use, and uses `-1` as a safe default epoch when the manager is absent.

**Before/After**

```scala
// Before – unconditional call, NPE if startup failed early
lifecycleManager.beginControlledShutdown()
...
lifecycleManager.beginShutdown()
...
logManager.shutdown(lifecycleManager.brokerEpoch)

// After – null-guarded
if (lifecycleManager != null) {
    lifecycleManager.beginControlledShutdown()
    ...
}
if (lifecycleManager != null)
    lifecycleManager.beginShutdown()
...
val brokerEpoch = if (lifecycleManager != null) lifecycleManager.brokerEpoch else -1
logManager.shutdown(brokerEpoch)
```

**How to spot**: In multi-phase startup sequences, each resource that may not be initialised must be null-checked in the paired shutdown path. Startup failures that occur before all resources are created will reach the shutdown path with partially-initialised state.

**Test**: No dedicated test added; the fix is verified by the observation that the broker now shuts down cleanly (logging the original `InvalidConfigurationException`) rather than crashing with a secondary `NullPointerException`.
