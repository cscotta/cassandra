# Bug 0371: Empty Rows Prematurely Skipped During Short-Read Protection

**Commit**: 24346d1789 · **Ticket**: CASSANDRA-16671
**Subsystem**: `src/java/org/apache/cassandra/db/SinglePartitionReadCommand.java`
**Category**: Logic error in condition

---

## Root Cause

`SinglePartitionReadCommand.isRowComplete` contained two compounding errors that caused live, CQL-table rows consisting entirely of tombstoned cells to be declared "incomplete" and therefore unnecessarily re-read from older SSTables, and in some cases caused valid rows to be dropped. The first error was an inverted condition on `primaryKeyLivenessInfo`: the code returned `false` (row not complete) when the liveness info was *present* and newer than the SSTable timestamp, but it should return `false` only when the liveness info is *absent* or too old. The second error was that `hasLiveCell` tracked whether at least one non-tombstone cell existed, and the final return value guarded on `hasLiveCell || !isCQLTable()` — meaning an all-tombstone-cell CQL row would never be considered complete even though such rows are semantically valid (they represent deletions).

### Before / After

```java
// BEFORE – inverted liveness guard; requires at least one non-tombstone cell
if (!row.primaryKeyLivenessInfo().isEmpty() && row.primaryKeyLivenessInfo().timestamp() <= sstableTimestamp)
    return false;
// ...
return hasLiveCell || !metadata().isCQLTable();
```

```java
// AFTER – correct: return false when liveness absent OR too old; no live-cell requirement
if (metadata().isCQLTable() && (row.primaryKeyLivenessInfo().isEmpty() || row.primaryKeyLivenessInfo().timestamp() <= sstableTimestamp))
    return false;
// ...
return true;
```

---

## How to Spot

Boolean conditions that combine `!isEmpty()` with `&&` on a timestamp comparison are suspicious: the intent is usually "if the field is present, check its age", which requires the emptiness check on the *opposite* side of the short-circuit. When reviewing completeness/dominance predicates for SSTable merge logic, mentally walk through the "all cells are tombstones" case and the "liveness info is absent" case separately to verify the predicate is correct in both.

---

## Test

`SinglePartitionReadCommandTest` adds a multi-SSTable scenario where an all-tombstone row must still be returned; `SSTablesIteratedTest` verifies the correct number of SSTables is consulted for various row-completeness configurations.
