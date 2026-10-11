# Bug 1543: Fix Tracing of Local Portions of Range Slices

**Commit**: 638c40e817 · **Ticket**: CASSANDRA-7599
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Wrong constant / default value

---

## Root Cause

`LocalRangeSliceRunnable` was constructed with `MessagingService.Verb.READ` instead of `MessagingService.Verb.RANGE_SLICE`. The verb is used by the tracing subsystem to associate the task with the correct trace stage. Using `READ` caused range-slice work executed locally on the coordinator to be attributed to the wrong trace stage or to produce no trace event at all. Additionally, the executor submission passed no `TraceState`, so the local runnable did not inherit the active trace session.

### Before / After

```java
// BEFORE — wrong verb, no trace state forwarded
LocalRangeSliceRunnable(AbstractRangeCommand command, ReadCallback<...> handler)
{
    super(MessagingService.Verb.READ);  // wrong
    ...
}
StageManager.getStage(Stage.READ).execute(new LocalRangeSliceRunnable(nodeCmd, handler));
```

```java
// AFTER — correct verb, trace state captured at submit time
LocalRangeSliceRunnable(AbstractRangeCommand command, ReadCallback<...> handler)
{
    super(MessagingService.Verb.RANGE_SLICE);  // correct
    ...
}
StageManager.getStage(Stage.READ).execute(
    new LocalRangeSliceRunnable(nodeCmd, handler), Tracing.instance.get());
```

---

## How to Spot

When a `Runnable` subclass is constructed with a `Verb` that identifies the operation type for tracing, verify the verb matches the actual command being run. Also check that executor `execute()` calls that should participate in tracing pass the current `TraceState` as the second argument.

---

## Test

No test added.
