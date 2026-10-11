# Bug 2078: Write Response Handler Ignores Pending Bootstrap Endpoints in Consistency Accounting

**Commit**: 6577baeb13 · **Ticket**: CASSANDRA-5354
**Subsystem**: src/java/org/apache/cassandra/service/AbstractWriteResponseHandler.java, src/java/org/apache/cassandra/service/DatacenterSyncWriteResponseHandler.java, src/java/org/apache/cassandra/service/WriteResponseHandler.java
**Category**: Logic error in condition

---

## Root Cause

During a bootstrap, writes must be sent to both natural and pending endpoints to ensure the bootstrapping node receives the data. However, the write-response handlers used only `consistencyLevel.blockFor(table)` — the number of natural replicas — as the required ack count, ignoring the extra pending endpoints. This meant the coordinator could declare a write successful without having received acknowledgements from all required nodes, violating consistency guarantees. Additionally, `DatacenterSyncWriteResponseHandler.ackCount()` computed acks incorrectly by deriving them from the per-DC countdown counters (which were initialised only for natural replicas), making it impossible to ever correctly count acks when pending endpoints existed.

### Before / After

```java
// BEFORE — WriteResponseHandler constructor
responses = new AtomicInteger(consistencyLevel.blockFor(table));

// BEFORE — WriteResponseHandler.ackCount()
return consistencyLevel.blockFor(table) - responses.get();

// BEFORE — DatacenterSyncWriteResponseHandler.ackCount() derived from counters
// that were never incremented for pending endpoints
```

```java
// AFTER — shared helper method
protected int totalBlockFor() {
    return consistencyLevel.blockFor(table) + pendingEndpoints.size();
}

// WriteResponseHandler constructor
responses = new AtomicInteger(totalBlockFor());

// DatacenterSyncWriteResponseHandler tracks acks directly
private final AtomicInteger acks = new AtomicInteger(0);
// per-DC counters incremented for pending endpoints at construction time
```

---

## How to Spot

Whenever a consistency-level check uses a hard-coded `blockFor()` call, verify that pending/bootstrap endpoints are also factored in. Any place that counts required acknowledgements without consulting `pendingEndpoints.size()` is a candidate for this bug during topology changes.

---

## Test

No test added.
