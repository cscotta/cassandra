# Bug 1538: Fix Merge — Wrong RangeTombstone Timestamp Accessors

**Commit**: cde5666290 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db, src/java/org/apache/cassandra/io/sstable
**Category**: Wrong constant / default value

---

## Root Cause

A bad merge introduced calls to non-existent (or semantically wrong) methods `RangeTombstone.minTimestamp()` and `RangeTombstone.maxTimestamp()` in three places: `ColumnFamily`, `LazilyCompactedRow`, and `SSTableWriter`. The correct API is the single `timestamp()` accessor. Using the wrong methods would cause incorrect min/max timestamp metadata to be tracked for range tombstones in SSTable metadata.

### Before / After

```java
// BEFORE
minTimestampSeen = Math.min(minTimestampSeen, rangeTombstone.minTimestamp());
maxTimestampSeen = Math.max(maxTimestampSeen, rangeTombstone.maxTimestamp());
```

```java
// AFTER
minTimestampSeen = Math.min(minTimestampSeen, rangeTombstone.timestamp());
maxTimestampSeen = Math.max(maxTimestampSeen, rangeTombstone.timestamp());
```

---

## How to Spot

After a merge, grep for method calls on domain objects that do not exist in the current API. Symmetric min/max patterns using different method names on the same object are a red flag for a copy-paste or merge error.

---

## Test

No test added.
