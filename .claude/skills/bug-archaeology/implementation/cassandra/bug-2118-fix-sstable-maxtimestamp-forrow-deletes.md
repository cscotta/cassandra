# Bug 2118: SSTable maxTimestamp ignores row-level tombstones and treats untracked timestamps as minimum

**Commit**: 9c8d949301 · **Ticket**: CASSANDRA-5153
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LazilyCompactedRow.java, src/java/org/apache/cassandra/io/sstable/SSTableMetadata.java
**Category**: Wrong constant / default value

---

## Root Cause

Two related defects caused SSTable `maxTimestamp` to be recorded incorrectly. First, in `LazilyCompactedRow`, the `maxTimestamp` was derived solely from column-level timestamps (`reducer.maxTimestampSeen`); if a row had a row-delete tombstone but no surviving columns, the reducer was null and `maxTimestamp` fell back to `Long.MIN_VALUE`, completely ignoring the row tombstone's timestamp. Second, in `SSTableMetadata` deserialization, pre-1.1.1 SSTables that do not track timestamps had their value forced to `Long.MIN_VALUE`, but the correct sentinel for "unknown / unreliable" is `Long.MAX_VALUE` so that compaction conservatively keeps those SSTables rather than incorrectly believing them to be older than everything else.

### Before / After

```java
// BEFORE (LazilyCompactedRow.java)
maxTimestamp = reducer == null ? Long.MIN_VALUE : reducer.maxTimestampSeen;
```

```java
// AFTER
long rowTombstone = emptyColumnFamily.getMarkedForDeleteAt();
maxTimestamp = reducer == null ? rowTombstone : Math.max(rowTombstone, reducer.maxTimestampSeen);
```

```java
// BEFORE (SSTableMetadata.java)
if (!desc.tracksMaxTimestamp)
    maxTimestamp = Long.MIN_VALUE;
```

```java
// AFTER
if (!desc.tracksMaxTimestamp)
    maxTimestamp = Long.MAX_VALUE;
```

---

## How to Spot

When computing a "maximum" across multiple sources, ensure all sources are included — here the row-level deletion timestamp was an overlooked source. For "unknown" sentinels used in comparisons, reason about whether the unknown value should be treated as smallest (MIN_VALUE) or largest (MAX_VALUE), choosing the conservative option for correctness.

---

## Test

No test added.
