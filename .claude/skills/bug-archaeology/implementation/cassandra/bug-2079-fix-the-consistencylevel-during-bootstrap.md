# Bug 2079: Write Response Handler Ignores Pending Bootstrap Endpoints in Consistency Accounting (1.1 branch)

**Commit**: 4d29c709a8 · **Ticket**: CASSANDRA-5354
**Subsystem**: src/java/org/apache/cassandra/service/AbstractWriteResponseHandler.java, src/java/org/apache/cassandra/service/DatacenterSyncWriteResponseHandler.java, src/java/org/apache/cassandra/service/WriteResponseHandler.java
**Category**: Logic error in condition

---

## Root Cause

This is the same bug as CASSANDRA-5354 applied to the 1.2 maintenance branch: during bootstrap, write-response handlers used only `consistencyLevel.blockFor(table)` for consistency accounting, ignoring the additional pending endpoints that also need to acknowledge writes. The `DatacenterSyncWriteResponseHandler` further had a broken `ackCount()` implementation that computed acks by subtracting from per-DC counters initialised only for natural replicas, making the pending-endpoint acks invisible to the consistency check.

### Before / After

```java
// BEFORE — WriteResponseHandler
responses = new AtomicInteger(consistencyLevel.blockFor(table));
// ackCount: return consistencyLevel.blockFor(table) - responses.get();

// BEFORE — DatacenterSyncWriteResponseHandler.ackCount()
// derived only from per-DC countdown counters, ignores pending endpoints
```

```java
// AFTER — shared helper
protected int totalBlockFor() {
    return consistencyLevel.blockFor(table) + pendingEndpoints.size();
}
// WriteResponseHandler: responses = new AtomicInteger(totalBlockFor());
// DatacenterSyncWriteResponseHandler: direct AtomicInteger acks counter
// per-DC counters pre-incremented for each pending endpoint at construction
```

---

## How to Spot

On any branch that received a consistency-during-bootstrap fix, confirm the same fix was applied to all sibling branches. Bootstrap-related consistency bugs affect all active maintained versions and must be backported consistently.

---

## Test

No test added.
