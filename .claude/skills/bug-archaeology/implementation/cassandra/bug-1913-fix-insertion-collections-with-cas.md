# Bug 1913: CAS Collection Insert Overwrites With Same Timestamp

**Commit**: fb55849795 · **Ticket**: CASSANDRA-6069
**Subsystem**: src/java/org/apache/cassandra/service/paxos/Commit.java
**Category**: Off-by-one

---

## Root Cause

When applying a CAS (Paxos) commit that inserts a collection, a range tombstone is first written to delete the previous collection values, then the new values are written—both using the same microsecond timestamp derived from the ballot UUID. Because tombstones win over regular inserts at equal timestamps in Cassandra's conflict resolution rules, the range tombstone unconditionally killed the new values, leaving the collection empty after every CAS write.

The fix is to stamp the deletion info with `t-1` so the new column values (stamped `t`) always win over the tombstones that clear the old collection.

### Before / After

```java
// BEFORE
cf.deletionInfo().updateAllTimestamp(t);
```

```java
// AFTER
// tombstones get t-1 so new collection values at t always win
cf.deletionInfo().updateAllTimestamp(t-1);
```

---

## How to Spot

When a write path issues a range tombstone followed by column inserts in the same logical operation, verify that the tombstone timestamp is strictly less than the column timestamp. Equal timestamps cause tombstones to win due to CRDT ordering rules.

---

## Test

No test added.
