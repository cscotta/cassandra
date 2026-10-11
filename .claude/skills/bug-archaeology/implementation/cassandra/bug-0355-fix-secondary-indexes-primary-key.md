# Bug 0355: Secondary Index on Primary Key Columns Skips Writes After Expired Row

**Commit**: c76ff1ba14 · **Ticket**: CASSANDRA-16868
**Subsystem**: src/java/org/apache/cassandra/index/internal (CassandraIndex)
**Category**: Logic error in condition

---

## Root Cause

When indexing a write on a primary key column, `CassandraIndex.indexer` called `getPrimaryKeyIndexLiveness()` (which computes liveness from the row's cells, taking the max timestamp among live cells but without coupling the TTL to that timestamp).  The old code passed `newRow.primaryKeyLivenessInfo()` directly, which reflected the row-level liveness token — when that token was expired (TTL elapsed), `primaryKeyLivenessInfo()` reported the row as dead even if a subsequent non-expiring write had refreshed a column.  Additionally, inside `getPrimaryKeyIndexLiveness`, the TTL was incorrectly tied to whichever cell had the highest timestamp: `ttl` was only updated when a new max timestamp was found, so it could reflect the TTL from a different, earlier cell rather than the one with the maximum timestamp.

### Before / After

```java
// BEFORE  (indexer, primary key path)
indexPrimaryKey(newRow.clustering(),
                newRow.primaryKeyLivenessInfo(),   // may be expired
                newRow.deletion());

// BEFORE  (getPrimaryKeyIndexLiveness inner loop)
if (cell.isLive(nowInSec)) {
    if (cellTimestamp > timestamp) {
        timestamp = cellTimestamp;
        ttl = cell.ttl();   // TTL coupled to max-timestamp cell
    }
}
```

```java
// AFTER  (indexer, primary key path)
indexPrimaryKey(newRow.clustering(),
                getPrimaryKeyIndexLiveness(newRow),   // derived from live cells
                newRow.deletion());

// AFTER  (getPrimaryKeyIndexLiveness inner loop)
if (cell.isLive(nowInSec)) {
    if (cellTimestamp > timestamp)
        timestamp = cellTimestamp;   // TTL not updated here; stays at default 0
}
```

---

## How to Spot

When deciding whether to emit an index entry for a primary key, code must not rely on the row's own `LivenessInfo` because that token can be expired even while individual cells are live.  Any index code that checks liveness for a primary key column should derive its timestamp from the live cells in the row, not from the row-level marker.  Also watch for accidental coupling of TTL to a timestamp comparison: updating `ttl` inside an `if (newTimestamp > old)` branch means the TTL belongs to the winning-timestamp cell, not the current cell.

---

## Test

Tests write a row with a TTL on a non-PK column, let it expire, then write a non-expiring update; asserts the index query returns the row.  Also tests the flush-before-update variant and the case where a delete precedes an update.
