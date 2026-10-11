# Bug 010: Fix disk usage guardrail disablement when failure threshold is reached

**Commit**: c79f176685 · **Ticket**: CASSANDRA-21057
**Subsystem**: Guardrails / Disk Usage Monitoring
**Category**: State not cleaned up

---

## Root Cause

`DiskUsageMonitor` runs a periodic task that skips all work when the guardrail is disabled (`!Guardrails.localDataDiskUsage.enabled(null)`). The problem is that when the guardrail transitions from enabled to disabled—specifically after a node has already broadcast a `FULL` disk state to peers—the monitor never notified other subsystems of the change. The `localState` field remained at `DiskUsageState.FULL` and was never reset, so even after an operator disabled the guardrail (e.g., by calling `setDataDiskUsagePercentageThreshold(-1, -1)`), writes continued to be rejected because the broadcaster still saw the old `FULL` state from the node.

### Before / After

```java
// BEFORE — simply returns on every tick when disabled; never clears old state
ScheduledExecutors.scheduledTasks.scheduleAtFixedRate(() -> {
    if (!Guardrails.localDataDiskUsage.enabled(null))
        return;
    updateLocalState(getDiskUsage(), notifier);
}, 0, intervalMs, TimeUnit.MILLISECONDS);

// AFTER — detects enabled→disabled transition and resets state to NOT_AVAILABLE
ScheduledExecutors.scheduledTasks.scheduleAtFixedRate(() -> {
    boolean currentEnabled = Guardrails.localDataDiskUsage.enabled(null);
    boolean oldEnabled = enabled;
    enabled = currentEnabled;
    boolean isDisabled = !currentEnabled && oldEnabled;
    if (isDisabled)
    {
        onDiskUsageGuardrailDisabled(notifier);
    }
    if (currentEnabled)
    {
        updateLocalState(getDiskUsage(), notifier);
    }
}, 0, intervalMs, TimeUnit.MILLISECONDS);

private void onDiskUsageGuardrailDisabled(Consumer<DiskUsageState> notifier)
{
    localState = DiskUsageState.NOT_AVAILABLE;
    notifier.accept(DiskUsageState.NOT_AVAILABLE);
}
```

---

## How to Spot

Any feature-flag-gated periodic task that publishes state to other subsystems must handle the "flag turned off" transition explicitly, not just skip execution. When a periodic task short-circuits on `!enabled`, ask: "Is there any state this task previously published that now needs to be retracted or reset?" If yes, the disablement transition needs its own cleanup path—a simple early return is insufficient.

---

## Test

`GuardrailDiskUsageTest.testDiskUsageNodetoolDisableWhenDiskIsFullShouldEnableWrites` simulates a two-node cluster where node 2 is injected into a `FULL` disk state, verifies that writes targeting it fail, then disables the guardrail and asserts that writes succeed again after the state disseminates; it then re-enables the guardrail and confirms failures return.
