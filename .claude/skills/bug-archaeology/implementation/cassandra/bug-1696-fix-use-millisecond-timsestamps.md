# Bug 1696: Fix batchlogManager#deleteBatch() use of millisecond timestamps

**Commit**: dc3f22b61d · **Ticket**: CASSANDRA-6822
**Subsystem**: src/java/org/apache/cassandra/db/BatchlogManager.java
**Category**: Wrong constant / default value

---

## Root Cause

`BatchlogManager.deleteBatch()` passed a millisecond timestamp to `RowMutation.delete()`, but Cassandra's internal mutation API expects timestamps in **microseconds**. Using milliseconds produces a timestamp three orders of magnitude too small, so the deletion tombstone would be sorted far in the past and would either fail to supersede existing data or interact incorrectly with TTL/GC-grace calculations.

### Before / After

```java
// BEFORE
mutation.delete(new QueryPath(SystemTable.BATCHLOG_CF, null, null), System.currentTimeMillis());
```

```java
// AFTER
mutation.delete(new QueryPath(SystemTable.BATCHLOG_CF, null, null), FBUtilities.timestampMicros());
```

---

## How to Spot

Whenever a timestamp is passed to a Cassandra internal mutation or comparator, verify the unit: internal APIs uniformly expect microseconds, but `System.currentTimeMillis()` silently compiles and produces milliseconds. Code-review checklist: reject any use of `System.currentTimeMillis()` in mutation/delete paths; require `FBUtilities.timestampMicros()` or `System.nanoTime()/1000`.

---

## Test

The existing `BatchlogManagerTest` was extended with an assertion that verifies the batch is actually removed after `deleteBatch()` is called, catching the case where the too-small tombstone timestamp left the entry visible.
