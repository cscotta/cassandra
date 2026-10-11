# Bug 2072: Wrong Default for min/max Timestamp in SSTableMetadata Breaks Timestamp-Bound Queries

**Commit**: 0439724179 · **Ticket**: CASSANDRA-5372
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableMetadata.java
**Category**: Wrong constant / default value

---

## Root Cause

When reading an SSTable whose format version does not track the minimum timestamp, the code assigned `Long.MIN_VALUE` as the default for `minTimestamp` and then immediately overrode it with `Long.MAX_VALUE` via a redundant branch — but that override was accidentally applied to `minTimestamp` while `maxTimestamp` received `Long.MIN_VALUE`. The correct sentinel for "not tracked" in both cases is `Long.MAX_VALUE`, because the metadata is used to skip SSTables whose max timestamp is below a query's range; `Long.MIN_VALUE` causes the SSTable to appear to contain no recent data and get incorrectly skipped.

### Before / After

```java
// BEFORE
long minTimestamp = desc.version.tracksMinTimestamp ? dis.readLong() : Long.MIN_VALUE;
if (!desc.version.tracksMinTimestamp)
    minTimestamp = Long.MAX_VALUE;
long maxTimestamp = desc.version.containsTimestamp() ? dis.readLong() : Long.MIN_VALUE;
```

```java
// AFTER
long minTimestamp = desc.version.tracksMinTimestamp ? dis.readLong() : Long.MIN_VALUE;
long maxTimestamp = desc.version.containsTimestamp() ? dis.readLong() : Long.MAX_VALUE;
```

---

## How to Spot

When a default/fallback value is used for a field that gates data-visibility decisions, verify the sentinel is the "inclusive" extreme (not the "exclusive" one). For timestamps where `Long.MAX_VALUE` means "unbounded / unknown max", initialising to `Long.MIN_VALUE` inverts the guard and causes false negatives.

---

## Test

No test added.
