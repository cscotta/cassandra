# Bug 1744: Fix compaction_history Timestamps

**Commit**: 319b42e58c · **Ticket**: CASSANDRA-6784
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionTask.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompactionTask` recorded the compaction start time in a local variable `start` (measured in milliseconds via `System.currentTimeMillis()`) and then passed that variable to `SystemKeyspace.updateCompactionHistory()`. However, the variable `start` was actually set elsewhere in the method to track elapsed time in nanoseconds (or was never assigned the wall-clock time at all), causing the timestamp stored in the `compaction_history` virtual table to be wrong. The fix replaces the stale `start` variable with a fresh `System.currentTimeMillis()` call at the point of recording.

### Before / After

```java
// BEFORE
SystemKeyspace.updateCompactionHistory(cfs.keyspace.getName(), cfs.name, start, startsize, endsize, mergedRows);
```

```java
// AFTER
SystemKeyspace.updateCompactionHistory(cfs.keyspace.getName(), cfs.name, System.currentTimeMillis(), startsize, endsize, mergedRows);
```

---

## How to Spot

When a variable intended to carry a wall-clock timestamp is reused or shadowed by a duration or monotonic counter, the wrong value silently propagates to storage. During review, verify that every timestamp argument passed to a persistence call is sourced directly from `System.currentTimeMillis()` (or an equivalent) rather than from a variable that may have been repurposed.

---

## Test

No test added.
