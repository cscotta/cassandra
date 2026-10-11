# Bug 0551: Mixed-Mode Range Scan Limit Counts Rows Instead of Partitions for Compact Static Tables

**Commit**: d27c3ad0d2 · **Ticket**: CASSANDRA-15072
**Subsystem**: `src/java/org/apache/cassandra/db/filter/DataLimits.java`, `db/ReadCommand.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In a mixed 2.x/3.x cluster, partition range scans on COMPACT STORAGE tables without clustering columns (static compact tables) coordinated by a 2.x node are serialized one CQL row per cell. The 3.x node receiving the scan applied a standard `CQLLimits` counter, which counts individual rows towards the `LIMIT` rather than counting whole partitions. As a result a `SELECT ... LIMIT N` on such a table returned fewer partitions than requested — each cell consumed one slot of the limit, so multi-column partitions were truncated early. The fix introduces a `legacyCompactStaticCqlLimits` variant that overrides `applyToRow` to be a no-op so that only complete partitions (counted at the partition boundary) consume the limit.

### Before / After

```java
// BEFORE (ReadCommand.java) - static compact tables fell through to normal CQL limits
else
    limits = DataLimits.cqlLimits(maxResults);
```

```java
// AFTER
else if (metadata.isStaticCompactTable())
    limits = DataLimits.legacyCompactStaticCqlLimits(maxResults);
else
    limits = DataLimits.cqlLimits(maxResults);
```

```java
// AFTER (DataLimits.java) - rows don't count; only partitions count
public Row applyToRow(Row row) {
    return row;  // noop: only count full partitions
}
```

---

## How to Spot

When a `LIMIT` clause produces too few results during rolling upgrades, check whether the counting granularity (row vs. partition) matches the on-wire representation used by the older node. Mixed-mode paths often require a separate `DataLimits` strategy because the legacy wire format maps the logical model differently. A code-review heuristic: every `isStaticCompactTable()` / `isCompactTable()` branch in query execution should be audited for whether its limit accounting matches the actual row-to-partition ratio of the legacy format.

---

## Test

New `CompactStorage2to3UpgradeTest` runs a 2.2-to-3.0 rolling upgrade scenario on a 3-node cluster and asserts that `SELECT * FROM ks.tbl LIMIT 2` returns exactly 2 partitions (single-column and multi-column variants).
