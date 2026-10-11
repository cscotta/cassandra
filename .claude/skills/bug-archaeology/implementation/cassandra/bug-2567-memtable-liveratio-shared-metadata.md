# Bug 2567: Memtable liveRatio inflated by counting shared CFMetadata for every row

**Commit**: cea1ada8c9 · **Ticket**: CASSANDRA-3203

## Detection Rule
When a memory-measuring agent traverses the object graph from each row in a memtable and the same `CFMetaData` singleton is reachable from every row, its memory is counted once per row instead of once, causing liveRatio to be wildly inflated.

## Code Smell
A `MemoryMeter` is a `static final` field initialized without a `withTrackerProvider` that excludes shared objects; every call to `measure()` on a row will re-traverse the shared `CFMetaData` reference, counting it N times.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: shared-object-double-counted
- severity: performance
- subsystem: storage-engine

## Root Cause
`Memtable` had a single `static final MemoryMeter meter` without an exclusion set. When computing `liveRatio`, the meter traversed from every row into the shared `CFMetaData` object and counted its memory each time. For a table with many rows, `CFMetaData` (which includes column definitions and other metadata) would be counted thousands of times, massively inflating the estimated live size, leading to premature flushes. The fix makes `meter` an instance field and passes a `trackerProvider` that adds `cfs.metadata` to the seen-objects set before each measurement.

## Example
```java
// BEFORE: shared metadata counted once per row
private static final MemoryMeter meter =
    new MemoryMeter().omitSharedBufferOverhead();
```
```java
// AFTER: exclude metadata from per-row traversal
meter = new MemoryMeter().omitSharedBufferOverhead()
    .withTrackerProvider(() -> {
        Set<Object> set = Collections.newSetFromMap(
            new IdentityHashMap<>());
        set.add(Memtable.this.cfs.metadata);
        return set;
    });
```
