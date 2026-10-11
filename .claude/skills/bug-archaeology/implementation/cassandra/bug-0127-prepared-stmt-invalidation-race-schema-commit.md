# Bug 0127: Race Between Prepared Statement Invalidation and Schema Commit

**Commit**: 9bf1680b1f · **Ticket**: CASSANDRA-20116
**Subsystem**: src/java/org/apache/cassandra/schema
**Category**: Race condition / missing synchronization

---

## Root Cause

Schema changes in Cassandra's TCM (Transaction Commit Manager) path went through `DistributedSchema.initializeKeyspaceInstances()`, which both applied the structural schema mutation (creating/dropping/altering keyspace instances) *and* fired `schemaChangeNotifier` events. Prepared statement invalidation is triggered by those notifier events. The race occurred because `initializeKeyspaceInstances` was called *before* the schema change was fully committed: if another thread executed a prepared statement in the window between invalidation and commit completion, it would reprepare the statement against the old (pre-commit) `TableMetadata` and cache that stale metadata permanently. The fix separates mutation from notification: structural mutations happen inside `initializeKeyspaceInstances`, while notifications (including prepared-statement invalidation) are deferred to a new `notifyPostCommit()` method called only after the commit is recorded in `SchemaListener`.

### Before / After

```java
// BEFORE – notifications (including prepared-statement invalidation) fired inside
// initializeKeyspaceInstances, BEFORE the schema change was committed
ksDiff.dropped.forEach(metadata -> {
    schemaChangeNotifier.notifyKeyspaceDropped(metadata, loadSSTables);
    dropKeyspace(metadata, true);
});
ksDiff.created.forEach(metadata -> {
    schemaChangeNotifier.notifyKeyspaceCreated(metadata);
    keyspaceInstances.put(...);
});
```

```java
// AFTER – notifications moved to notifyPostCommit(), called by SchemaListener
//         only after the change is fully committed
ksDiff.dropped.forEach(metadata -> dropKeyspace(metadata, true));
ksDiff.created.forEach(metadata -> keyspaceInstances.put(...));
// ... then, post-commit:
public void notifyPostCommit(DistributedSchema prevSchema, boolean loadSSTables) {
    ksDiff.dropped.forEach(m -> schemaChangeNotifier.notifyKeyspaceDropped(m, loadSSTables));
    ksDiff.created.forEach(schemaChangeNotifier::notifyKeyspaceCreated);
    ...
}
```

---

## How to Spot

Any pattern where cache invalidation (or other "cleanup" notifications) fires before the state it is supposed to reflect has been committed is a classic TOCTOU race. During review, look for notifier/listener calls that are interleaved with the mutation they describe: invalidations should always follow, never precede, the point at which the new state becomes visible to all readers.

---

## Test

`PreparedStatementInvalidationRaceTest.testInvalidationRace` uses ByteBuddy to pause `initializeKeyspaceInstances` mid-execution, executes a prepared statement in the gap, then releases the latch and asserts that the cached prepared statement on the server side references the *new* `TableMetadata` epoch, confirming no stale reprepare occurred.
