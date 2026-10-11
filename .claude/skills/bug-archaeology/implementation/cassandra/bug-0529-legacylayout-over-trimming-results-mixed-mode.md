# Bug 0529: LegacyLayout Over-Trims Results in Mixed Mode Clusters

**Commit**: f0aa60bec7 · **Ticket**: CASSANDRA-15405
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a 3.x coordinator serializes a read result back to a 2.x node in a mixed-mode cluster, it applies a cell count limit before sending. The original `maxCellsPerPartition` logic blindly truncated the cell list to the first N cells regardless of whether those cells were live or expired/tombstoned. Because dead cells count toward the limit, fewer live results than the query requested could be returned, effectively over-trimming the response and making queries appear to return fewer rows than exist.

### Before / After

```java
// BEFORE
int maxCellsPerPartition = maxCellsPerPartition(command);
if (cells.size() > maxCellsPerPartition)
    cells = cells.subList(0, maxCellsPerPartition);
```

```java
// AFTER
int maxCellsPerPartition = maxLiveCellsPerPartition(command);
cells = maybeTrimLiveCells(cells, maxCellsPerPartition, command);
// maybeTrimLiveCells counts only live cells toward the limit,
// preserving all dead cells encountered before the Nth live cell
```

---

## How to Spot

Whenever a result-set limiter iterates a mixed list of live and dead cells using a single counter, check whether tombstones should be counted the same way as live cells. In upgrade/mixed-mode serialization paths, the limit is defined in terms of visible (live) cells, not raw cell count, so any simple `subList(0, N)` without a liveness check will under-deliver results.

---

## Test

An in-JVM upgrade test between v2.2 and v3.0 verifies that queries return the expected row count after partial upgrade, catching the case where the 3.x node under-trims the result for a 2.x coordinator.
