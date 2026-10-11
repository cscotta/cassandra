# Bug 0600: Deadlock When Running nodetool refresh Before Node Is Fully Up

**Commit**: 22bb413ba2 · **Ticket**: CASSANDRA-14310
**Subsystem**: `src/java/org/apache/cassandra/db/ColumnFamilyStore.java`, `src/java/org/apache/cassandra/service/StorageService.java`
**Category**: Deadlock

---

## Root Cause

`ColumnFamilyStore.loadNewSSTables` was declared `synchronized` at the static method level, which means it holds the `ColumnFamilyStore` class lock. During normal startup, the bootstrap or initialization code also acquires locks that depend on `ColumnFamilyStore` being in a ready state. If an operator runs `nodetool refresh` before the node finishes initializing, the two lock acquisition paths could cross, producing a deadlock. The fix has two parts: remove the `synchronized` keyword from `loadNewSSTables` (because it was protecting state that does not actually need a class-level lock) and add an early guard in `StorageService.loadNewSSTables` that rejects calls issued before initialization completes.

### Before / After

```java
// BEFORE — class-level lock held across SSTable loading
public static synchronized void loadNewSSTables(String ksName, String cfName)
{
    Keyspace keyspace = Keyspace.open(ksName);
    ...
}
```

```java
// AFTER — lock removed; guard added in the public API layer
// StorageService.loadNewSSTables:
public void loadNewSSTables(String ksName, String cfName)
{
    if (!isInitialized())
        throw new RuntimeException("Not yet initialized, can't load new sstables");
    ColumnFamilyStore.loadNewSSTables(ksName, cfName);
}
// ColumnFamilyStore.loadNewSSTables: `synchronized` removed
public static void loadNewSSTables(String ksName, String cfName) { ... }
```

---

## How to Spot

When a `static synchronized` method acquires a class-level monitor and the same monitor is also reachable through the startup initialization path, any external entry point that bypasses the "are we ready?" guard can trigger a classic lock-order deadlock. Review any `static synchronized` method that opens keyspaces or column family stores and check whether startup code holds the same class monitor at any point in its call stack.

---

## Test

No test added.
