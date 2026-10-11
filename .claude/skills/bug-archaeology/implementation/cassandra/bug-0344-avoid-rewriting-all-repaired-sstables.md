# Bug 0344: Repaired SSTables Needlessly Rewritten During Cleanup When Transient Replication Is Disabled

**Commit**: 41b43a46c1 · **Ticket**: CASSANDRA-16966
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

The cleanup code checked `needsCleanupTransient` (whether the SSTable's data falls outside the node's transient ranges) independently of whether transient replication is even in use, and then applied the `isRepaired()` gate only when deciding to skip. The original condition `!needsCleanupFull && (!needsCleanupTransient || !sstable.isRepaired())` would skip an SSTable when it needed no full-range cleanup AND (it needed no transient cleanup OR it was unrepaired). However when transient ranges are empty (i.e. transient replication is disabled), `needsCleanup(sstable, emptyRanges)` still returns `true` because any token is "outside" an empty range, so `needsCleanupTransient` became unconditionally true for every SSTable. This caused all repaired SSTables to be rewritten during cleanup even when the node had no transient replicas.

### Before / After

```java
// BEFORE
boolean needsCleanupTransient = needsCleanup(sstable, transientRanges);
// ...
if (!needsCleanupFull && (!needsCleanupTransient || !sstable.isRepaired()))
    // skip

```

```java
// AFTER
boolean needsCleanupTransient = !transientRanges.isEmpty() && sstable.isRepaired() && needsCleanup(sstable, transientRanges);
// ...
if (!needsCleanupFull && !needsCleanupTransient)
    // skip
```

---

## How to Spot

When a boolean flag guards whether a secondary collection is in use, any range or set membership check against that collection must first verify the collection is non-empty (or the feature is enabled). Calling `needsCleanup(sstable, emptyRanges)` silently returns true because every token is outside an empty set of ranges, so the short-circuit guard must come before the predicate call.

---

## Test

Adds `testCleanupSkippingRepairedSSTables` to `CleanupTest`: marks all SSTables as repaired, then runs cleanup on a single-token node and asserts the live SSTable set is unchanged (i.e. no rewrite occurred).
