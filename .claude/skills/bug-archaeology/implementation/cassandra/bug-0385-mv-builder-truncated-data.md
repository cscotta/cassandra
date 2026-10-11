# Bug 0385: Materialized View Builders Inserting Truncated Data

**Commit**: ca40e770a5 · **Ticket**: CASSANDRA-16567
**Subsystem**: src/java/org/apache/cassandra/db/view
**Category**: Race condition / missing synchronization

---

## Root Cause

When a base table is truncated, the materialized view builder running in the background is not stopped before the truncation executes. The old `View.build()` method would call `builder.stop()` (a non-blocking signal) and immediately start a new builder, but it never waited for the in-flight builder to finish. This meant the truncated-but-still-running builder could write MV rows derived from data that had already been deleted by the truncation, leaving ghost rows in the materialized view. The fix adds `builder.waitForCompletion()` (backed by a `CountDownLatch`) so that `stopBuild()` is fully synchronous, and then calls `viewManager.stopBuild()` + `viewManager.build()` around the truncation in `ColumnFamilyStore`.

### Before / After

```java
// BEFORE – in View.build(): signals stop but does not wait
public synchronized void build()
{
    if (this.builder != null)
    {
        this.builder.stop();
        this.builder = null;
    }
    this.builder = new ViewBuilder(baseCfs, this);
    CompactionManager.instance.submitViewBuilder(builder);
}
```

```java
// AFTER – stopBuild() blocks until the builder has finished
synchronized void stopBuild()
{
    if (builder != null)
    {
        builder.stop();
        builder.waitForCompletion();  // new CountDownLatch-backed wait
        builder = null;
    }
}
// ColumnFamilyStore.truncateBlocking() now wraps with:
//   viewManager.stopBuild();
//   ... truncation ...
//   viewManager.build();
```

---

## How to Spot

Whenever a "stop and restart" pattern exists for a background builder/compactor, check that the stop is synchronous before any state-clearing operation (flush, truncate, drop) proceeds. A non-blocking `stop()` signal followed by immediate state destruction is a classic race: the still-running task reads the old state and writes its results after the cleanup has already committed.

---

## Test

`testTruncateWhileBuilding` in `test/unit/org/apache/cassandra/cql3/ViewTest.java` uses Byteman to block the builder at `buildKey` until `truncateBlocking` starts, then asserts the MV is empty both during and after the truncation.
