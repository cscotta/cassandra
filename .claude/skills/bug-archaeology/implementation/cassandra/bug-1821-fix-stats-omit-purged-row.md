# Bug 1820: Stats Incorrectly Using Raw Row Tombstone Timestamp

**Commit**: 0d8da2ee3c · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`LazilyCompactedRow` computed the max-timestamp for `ColumnStats` using `maxRowTombstone.markedForDeleteAt` directly. If the row tombstone had been purged (gc-eligible), `maxRowTombstone` still reflected the raw pre-purge deletion time, causing the SSTable stats to record an inflated maximum timestamp. The correct source of truth after purging is `emptyColumnFamily.maxTimestamp()`, which accounts for whether the tombstone survived the gc pass.

### Before / After

```java
// BEFORE – uses raw tombstone timestamp even if tombstone was purged
reducer == null ? maxRowTombstone.markedForDeleteAt
                : Math.max(maxRowTombstone.markedForDeleteAt, reducer.maxTimestampSeen),
```

```java
// AFTER – uses the already-purged ColumnFamily's max timestamp
reducer == null ? emptyColumnFamily.maxTimestamp()
                : Math.max(emptyColumnFamily.maxTimestamp(), reducer.maxTimestampSeen),
```

---

## How to Spot

When computing aggregate statistics after a purge step, always source timestamps from the already-purged data structure rather than from the intermediate tombstone object. A mismatch between "what was purged" and "what the stats reflect" causes downstream consumers (e.g., repair, bloom filters) to make incorrect decisions.

---

## Test

No test added in this commit; the fix is a one-liner with coverage expected from the broader compaction test suite updated in sibling commits.
