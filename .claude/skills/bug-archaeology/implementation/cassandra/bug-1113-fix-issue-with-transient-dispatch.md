# Bug 1113: Hint Dispatch Loop Does Not Break on Transient Node Failure

**Commit**: e6a9afbb8a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hints/HintsDispatchExecutor.java
**Category**: Logic error in condition

---

## Root Cause

When a destination node goes down mid-dispatch, the `HintsDispatcher` signals a partial delivery by returning `false` from its completion check, but the dispatch loop in `HintsDispatchExecutor` called `dispatch()` as a `void` method and discarded that result entirely. The loop therefore kept iterating through remaining hint files indefinitely — queuing them back with `offerFirst` — instead of stopping and waiting for the node to come back up. The fix changes `dispatch()` to return a `boolean` and adds an explicit `break` when `false` is returned.

### Before / After

```java
// BEFORE
private void dispatch(HintsDescriptor descriptor) { ... }

// call site — return value not possible, failure silently ignored
dispatch(descriptor);
```

```java
// AFTER
private boolean dispatch(HintsDescriptor descriptor) {
    ...
    return true;   // fully dispatched
    ...
    return false;  // node went down, partial only
}

// call site — breaks the loop on failure
if (!dispatch(descriptor))
    break;
```

---

## How to Spot

Whenever a loop drives a series of operations that can partially fail, verify that the inner worker communicates failure back to the loop controller. A `void` helper inside a dispatch or retry loop is a red flag: if the operation can signal success/failure, that information must flow upward. Review all loop bodies where the called method mutates a shared queue (`offerFirst`, `add`) on failure — if the loop never exits on such a call, it will spin forever.

---

## Test

No test added.
