# Bug 0271: NPE in CompactionController When neverPurgeTombstones Toggled from True to False

**Commit**: ff5f4833aa · **Ticket**: CASSANDRA-17897
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionController.java
**Category**: Missing null / bounds check

---

## Root Cause

`CompactionController` initialises `overlapping` (the set of overlapping SSTables) and `overlapIterator` only when `neverPurgeTombstones` is `false` at construction time. When the table property is `true` at construction the fields are left `null`. If the property is then changed to `false` while a compaction is in progress, the existing `CompactionController` instance has `null` fields; subsequent calls to `getFullyExpiredSSTables` and `getPurgeEvaluator`—which only gated on `neverPurgeTombstones` and not on nullness—would dereference `overlapping` or call `overlapIterator.update(key)` and throw a `NullPointerException`.

### Before / After

```java
// BEFORE – no null guard; NPE when overlapping/overlapIterator is null
if (NEVER_PURGE_TOMBSTONES || compacting == null || cfStore.getNeverPurgeTombstones())
    return Collections.<SSTableReader>emptySet();
// ... streams over `overlapping` unconditionally

if (NEVER_PURGE_TOMBSTONES || !compactingRepaired() || cfs.getNeverPurgeTombstones())
    return time -> false;
overlapIterator.update(key);   // NPE if null
```

```java
// AFTER – null checks added alongside the neverPurgeTombstones guard
if (NEVER_PURGE_TOMBSTONES || compacting == null || cfStore.getNeverPurgeTombstones() || overlapping == null)
    return Collections.emptySet();

if (NEVER_PURGE_TOMBSTONES || !compactingRepaired() || cfs.getNeverPurgeTombstones() || overlapIterator == null)
    return time -> false;
```

---

## How to Spot

Fields that are conditionally initialised (set only when a flag is `false`) must be guarded at every use site, not just at the primary flag check. Whenever a runtime configuration flag can be toggled on a live object, review all code paths that assume co-initialised fields are non-null simply because the flag is currently `false`.

---

## Test

`CompactionControllerTest.testDisableNeverPurgeTombstones` creates a `CompactionController` with `neverPurgeTombstones=true`, then toggles it to `false` and calls `getPurgeEvaluator` and `getFullyExpiredSSTables` to confirm no NPE is thrown.
