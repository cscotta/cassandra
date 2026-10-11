# Bug 2152: Schema Temporarily Missing After Upgrade from Pre-1.1.5

**Commit**: 3e27a320b4 · **Ticket**: CASSANDRA-5061
**Subsystem**: src/java/org/apache/cassandra/db/DefsTable.java
**Category**: State not cleaned up

---

## Root Cause

When upgrading from a pre-1.1.5 node, `DefsTable` would apply a fix-up mutation to correct schema timestamps, but the updated rows were not flushed to disk before the commit log was replayed. Because Cassandra reads schema before replaying the commit log at startup, the fixed-up schema lived only in the memtable and was temporarily invisible, causing a window where schema appeared missing.

### Before / After

```java
// BEFORE
mutation.apply();
// (no flush — schema fix stays in memtable)
```

```java
// AFTER
mutation.apply();
// flush immediately because we read schema before replaying the commitlog
try {
    cfs.forceBlockingFlush();
} catch (ExecutionException e) {
    throw new RuntimeException("Could not flush after fixing schema timestamps", e);
}
```

---

## How to Spot

When a startup migration path writes schema corrections, verify that any data needed before commit-log replay is flushed to disk immediately. A `mutation.apply()` alone is insufficient if the result must be durable before later startup phases read it.

---

## Test

No test added.
