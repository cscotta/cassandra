# Bug 1403: Fix incremental repair not remove parent session on remote

**Commit**: feb26752b2 · **Ticket**: CASSANDRA-8291
**Subsystem**: src/java/org/apache/cassandra/repair/RepairMessageVerbHandler.java
**Category**: State not cleaned up

---

## Root Cause

During incremental repair, the coordinating node creates a `ParentRepairSession` entry on every participating node via a `PREPARE_MESSAGE`. When anti-compaction completes on a remote node, the `ANTICOMPACTION_REQUEST` handler ran `doAntiCompaction` but never removed the session from `ActiveRepairService.parentRepairSessions`. Because cleanup only happened on the coordinator, remote nodes accumulated stale `ParentRepairSession` objects for every completed incremental repair run, leaking memory and potentially causing confusion on subsequent repairs that reused session UUIDs.

### Before / After

```java
// BEFORE
case ANTICOMPACTION_REQUEST:
    ...
    ActiveRepairService.instance.doAntiCompaction(anticompactionRequest.parentRepairSession, ...);
    // session entry left in parentRepairSessions map
    break;
```

```java
// AFTER
case ANTICOMPACTION_REQUEST:
    ...
    try {
        ActiveRepairService.instance.doAntiCompaction(anticompactionRequest.parentRepairSession, ...);
    } finally {
        ActiveRepairService.instance.removeParentRepairSession(anticompactionRequest.parentRepairSession);
    }
    break;
```

---

## How to Spot

Any time a resource or session is registered in a map by a remote initiator, look for the corresponding deregistration on every completion path — including both the success branch and the exception branch. The use of `finally` is the canonical signal that cleanup is guaranteed. In repair code, search for every `parentRepairSessions.put` and verify there is a matching `remove` reachable on all exit paths (normal and exceptional) of the handler that processes the session.

---

## Test

No test added.
