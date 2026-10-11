# Bug 0677: Secondary Indexes Always Rebuilt at Startup

**Commit**: 6e19e81db8 · **Ticket**: CASSANDRA-13725
**Subsystem**: `src/java/org/apache/cassandra/index/SecondaryIndexManager.java`
**Category**: State not cleaned up

---

## Root Cause

`SecondaryIndexManager.markIndexesBuilding()` always called `SystemKeyspace.setIndexRemoved()` when an index was first registered, including during initial ColumnFamilyStore construction at node startup. This erased the persisted "index is built" record even for indexes that had been fully built on previous runs, forcing a complete rebuild on every restart. The root issue was that the "mark building" step conflated two distinct situations: loading an existing index at startup (where the system-keyspace record should be preserved) and registering a brand-new index (where it must be cleared for a fresh build). The fix threads an `isNewCF` boolean through `addIndex` → `createIndex` → `markIndexesBuilding` and skips the `setIndexRemoved` call when loading at startup.

### Before / After

```java
// BEFORE – always removes the built-index record
if (counter.getAndIncrement() == 0 && DatabaseDescriptor.isDaemonInitialized())
    SystemKeyspace.setIndexRemoved(keyspaceName, indexName);
```

```java
// AFTER – skips removal when initializing from an existing table
if (counter.getAndIncrement() == 0 && DatabaseDescriptor.isDaemonInitialized() && !isNewCF)
    SystemKeyspace.setIndexRemoved(keyspaceName, indexName);
```

---

## How to Spot

Any initialization routine that resets persistent state (removing "already built" markers, clearing caches, zeroing counters) should distinguish between "first ever creation" and "reload of existing state." A single boolean parameter that disambiguates these two cases is a common and effective fix. Reviewers should look for `setIndexRemoved` or analogous "mark not done" calls that fire unconditionally during startup paths.

---

## Test

No test added (the change is verified indirectly by existing `SecondaryIndexTest` and `RangeTombstoneTest` tests that were updated to pass the new `isNewCF` argument).
