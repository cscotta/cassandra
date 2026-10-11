# Bug 0042: SAI Index Availability Gap Between Node UP and Queryability Marking on Bounce

**Commit**: f553acea3e · **Ticket**: CASSANDRA-20732
**Subsystem**: index/sai/StorageAttachedIndex
**Category**: Race condition / missing synchronization

---

## Root Cause

When a node bounced (restarted) with already-built SAI indexes, `getInitializationTask()` always launched a full initial build task even when no SSTables were missing index data. The build task first forced a memtable flush and then validated and marked the index queryable after the flush completed. Between the moment the node was marked UP (network-visible) and the moment the index was eventually marked queryable, queries using the SAI index would fail or be refused. The fix short-circuits `getInitializationTask()` during startup: if all SSTables are already indexed, the index is marked queryable immediately in the initialization task (before any flush or rebuild), eliminating the gap.

### Before / After

```java
// BEFORE — always queued a full build, leaving the index non-queryable until completion
IndexValidation validation = StorageService.instance.isStarting()
    ? IndexValidation.HEADER_FOOTER : IndexValidation.NONE;
return () -> startInitialBuild(baseCfs, validation).get();
```

```java
// AFTER — marks queryable immediately if already complete
if (isStarting) {
    Collection<SSTableReader> nonIndexed = findNonIndexedSSTables(baseCfs, indexGroup, validation);
    if (nonIndexed.isEmpty()) {
        baseCfs.indexManager.makeIndexQueryable(this, Status.BUILD_SUCCEEDED);
        initBuildStarted = true;
        return () -> ImmediateFuture.success(null);
    }
}
return () -> startInitialBuild(baseCfs, validation).get();
```

---

## How to Spot

Any index lifecycle path that defers queryability marking to the end of an asynchronous build task creates a window where clients can observe the node as UP but get query failures. Review `getInitializationTask` implementations to check whether they can fast-path to `makeIndexQueryable` when the persistent state is already complete, particularly on restart paths.

---

## Test

`StorageAttachedIndexDDLTest.shouldMarkQueryableInInitializationTask` creates an index, writes 10 SSTables, artificially marks the index non-queryable, sets the node to STARTING mode, calls `buildIndex`, then verifies queries succeed without running the full build.
