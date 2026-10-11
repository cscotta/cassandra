# Bug 2100: Multiple Concurrent Compactions Could Run on the Same SSTables

**Commit**: 686f516ccb · **Ticket**: CASSANDRA-5256
**Subsystem**: src/java/org/apache/cassandra/db/compaction/AbstractCompactionTask.java, src/java/org/apache/cassandra/db/compaction/CompactionManager.java, src/java/org/apache/cassandra/db/compaction/SizeTieredCompactionStrategy.java, src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The compaction marking and unmarking of SSTables was scattered across `CompactionManager`, compaction strategies, and callers, with no single owner responsible for the lifecycle. This made it possible for two concurrent threads to start compacting the same SSTables: one thread could mark them, execute, and unmark them, while a second thread raced to pick up the same candidates. The fix centralises unmarking inside `AbstractCompactionTask.execute` (via a `finally` block), removes the old `unmarkSSTables` method, and changes strategies to loop until they successfully mark a candidate set rather than bailing on the first failure. An assertion in the constructor verifies that sstables are already marked compacting when the task is built.

### Before / After

```java
// BEFORE – caller responsible for unmarking
try {
    task.execute(metrics);
} finally {
    task.unmarkSSTables();
}
```

```java
// AFTER – AbstractCompactionTask.execute owns the lifecycle
public int execute(CompactionExecutorStatsCollector collector) {
    try {
        return executeInternal(collector);
    } finally {
        cfs.getDataTracker().unmarkCompacting(sstables);
    }
}
```

---

## How to Spot

When resource acquisition (mark compacting) and release (unmark compacting) are performed in different classes with callers expected to match them manually, the contract is fragile. Centralising both into the resource owner — here `AbstractCompactionTask` — eliminates the class of bug where callers forget to unmark or where concurrent callers race on the same set.

---

## Test

Test helpers in `Util.java` and `CompactionsPurgeTest.java` were updated to go through `getUserDefinedTask` rather than constructing `CompactionTask` directly, enforcing the new marking contract.
