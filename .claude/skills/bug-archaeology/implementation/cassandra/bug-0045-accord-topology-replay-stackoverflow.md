# Bug 0045: StackOverflowError During Accord Topology Replay with Many Epochs

**Commit**: 259b1a532e · **Ticket**: CASSANDRA-20702
**Subsystem**: service/accord/AccordConfigurationService
**Category**: Missing override of default method

---

## Root Cause

`AccordConfigurationService` inherited the `executor()` method from `AbstractConfigurationService`, whose default implementation ran topology-replay callbacks directly on the calling thread using recursive or deeply-chained invocations. When a node started with a large number of accumulated active epochs in the TCM log, replaying all of them in sequence on the calling thread exhausted the JVM stack, producing a `StackOverflowError`. The fix overrides `executor()` to dispatch work onto the `ACCORD_MIGRATION` stage executor, moving replay off the calling thread and onto a thread-pool where deep call stacks are not an issue.

### Before / After

```java
// BEFORE — no override; default implementation runs callbacks on calling thread
// (inherited from AbstractConfigurationService)
```

```java
// AFTER — replay dispatched to a bounded executor
@Override
protected Executor executor()
{
    return Stage.ACCORD_MIGRATION::execute;
}
```

---

## How to Spot

When a `default` method in an abstract base class performs work directly on the calling thread without any thread-pool dispatch, subclasses that accumulate large backlogs of work are vulnerable to stack overflow. Any time a default base-class `executor()` or `execute()` method simply calls the callback inline, check whether high-cardinality replay or bulk initialization paths can exhaust the stack.

---

## Test

No dedicated test added; the fix is a structural change to move replay to an executor rather than the calling thread.
