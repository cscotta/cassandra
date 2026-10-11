# Bug 1482: Fix Failure to Stream Ranges When Moving

**Commit**: 3b3afb35cd · **Ticket**: CASSANDRA-7836
**Subsystem**: streaming / StorageService
**Category**: Logic error in condition

---

## Root Cause

`StreamPlan.isEmpty()` was implemented with an inverted boolean: it returned `coordinator.hasActiveSessions()` instead of `!coordinator.hasActiveSessions()`. A plan with active sessions is by definition *not* empty, so the method was returning the opposite of its contract. Any caller that relied on `isEmpty()` to decide whether to skip streaming would instead skip non-empty plans and attempt to execute empty ones, causing the node to silently fail to transfer ranges during a move operation.

### Before / After

```java
// BEFORE
public boolean isEmpty()
{
    return coordinator.hasActiveSessions();
}
```

```java
// AFTER
public boolean isEmpty()
{
    return !coordinator.hasActiveSessions();
}
```

---

## How to Spot

Boolean predicate names that start with `is` or `has` are easy to invert accidentally. During review, verify that the method body semantically matches its name: a method called `isEmpty()` must return `true` when the object has no contents.

---

## Test

No test added. The bug manifests as a silent no-op during a node move (ranges are never transferred), which is difficult to assert in a unit test; a cluster integration test would be required.
