# Bug 0635: Repair Race — Gossip Blocked by Synchronized prepareForRepair

**Commit**: 49edd70740 · **Ticket**: CASSANDRA-13849
**Subsystem**: `service/ActiveRepairService.java`
**Category**: Deadlock

---

## Root Cause

`prepareForRepair()` was declared `synchronized` and called `registerParentRepairSession()` internally; `registerParentRepairSession()` was not synchronized. When a repair message arrived from a coordinator, the `synchronized` lock on the `ActiveRepairService` instance was held for the entire duration of the prepare phase, which included gossip callbacks. If gossip simultaneously tried to call any other `synchronized` method on the same object, it would block until the repair prepare finished — causing gossip to stall. The fix moved the `synchronized` modifier from the outer `prepareForRepair` to the smaller, inner `registerParentRepairSession`, and added a containment check inside `registerParentRepairSession` to silently skip duplicate registrations caused by re-entrant calls.

### Before / After

```java
// BEFORE
public synchronized UUID prepareForRepair(...) {
    registerParentRepairSession(...);   // holds lock through gossip callbacks
    ...
}

public void registerParentRepairSession(...) {
    parentRepairSessions.put(parentRepairSession, new ParentRepairSession(...));
}
```

```java
// AFTER
public UUID prepareForRepair(...) {
    registerParentRepairSession(...);   // no outer lock
    ...
}

public synchronized void registerParentRepairSession(...) {
    if (!parentRepairSessions.containsKey(parentRepairSession)) {
        parentRepairSessions.put(parentRepairSession, new ParentRepairSession(...));
    }
}
```

---

## How to Spot

Coarse-grained `synchronized` methods that invoke callbacks or external subsystems (gossip, messaging, I/O) while holding the monitor are a classic deadlock risk. Reviewers should flag `synchronized` on long or multi-step methods and ask whether the lock can be narrowed to just the shared-state mutation.

---

## Test

No test added.
