# Bug 2219: NPE During ColumnFamilyStore Reload When Comparator Changes Concurrently

**Commit**: c8a7618763 · **Ticket**: CASSANDRA-4786
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/db/ColumnFamilyStore.java, src/java/org/apache/cassandra/db/Memtable.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When a column family's comparator type is altered, `ColumnFamilyStore.reload()` needs to flush and replace the current memtable so it is created with the new comparator. The original code called `maybeSwitchMemtable` once, but there was a race: another thread could flush the memtable between the time `reload` acquired the reference and when it submitted the flush future, meaning the new memtable might still carry the old comparator. Additionally, none of the `CFMetaData` fields were `volatile`, so a thread reading them after a concurrent schema change could see stale (null) values, causing an NPE.

The fix has two parts: all mutable `CFMetaData` fields are marked `volatile` to ensure cross-thread visibility of schema updates, and `ColumnFamilyStore.reload` loops until it confirms the current memtable was created with the updated comparator (using a new `initialComparator` field on `Memtable`).

### Before / After

```java
// BEFORE – single attempt, no visibility guarantee
maybeSwitchMemtable(getMemtableThreadSafe(), true).get();
```

```java
// AFTER – loop until memtable reflects the new comparator
while (true) {
    AbstractType comparator = metadata.comparator;
    Memtable memtable = getMemtableThreadSafe();
    if (memtable.initialComparator == comparator)
        break;
    Future future = maybeSwitchMemtable(getMemtableThreadSafe(), true);
    if (future != null)
        future.get();
}
```

---

## How to Spot

Any field shared between threads that is written by one thread and read by another must be `volatile` (or protected by a lock) to guarantee visibility. When a retry loop depends on a condition that another thread may also satisfy, confirm that the exit condition is checked after the awaited operation, not before.

---

## Test

No test added.
