# Bug 1103: Fix LWW Bug Affecting MaterializedViews

**Commit**: f00a3da835 · **Ticket**: CASSANDRA-10197
**Subsystem**: src/java/org/apache/cassandra/db/view/TemporalRow.java
**Category**: Logic error in condition

---

## Root Cause

`TemporalRow` tracked cell versions for Materialized View reconciliation using a `SortedMap<Long, TemporalCell>` keyed by timestamp. The Last-Write-Wins (LWW) resolvers (`earliest`, `latest`, `oldValueIfUpdated`) iterated this sorted map under the assumption that sorting by timestamp was sufficient to determine the winner. However, Cassandra's cell reconciliation also applies tie-breaking rules (e.g., by value) when timestamps are equal. By delegating to the map's natural ordering rather than to the real `TemporalCell.reconcile()` logic, the resolvers could pick the wrong cell when two updates arrived with the same timestamp. The fix replaces the `SortedMap` with a dedicated `TemporalCell.Versions` class that holds at most one existing cell and one new cell, and implements all three resolution strategies through explicit calls to `reconcile()`.

### Before / After

```java
// BEFORE — sorted by timestamp only, misses tie-breaking
private final Map<ColumnIdentifier, Map<CellPath, SortedMap<Long, TemporalCell>>> columnValues = new HashMap<>();
...
public static final Resolver latest = cells -> {
    TemporalCell value = iterator.next();
    while (iterator.hasNext())
        value = value.reconcile(iterator.next());
    return value;
};
```

```java
// AFTER — Versions class uses reconcile() directly
private final Map<ColumnIdentifier, Map<CellPath, TemporalCell.Versions>> columnValues = new HashMap<>();
...
public static final Resolver latest = TemporalCell.Versions::getLatestCell;
// getLatestCell calls existingCell.reconcile(newCell)
```

---

## How to Spot

When LWW semantics are needed, using a sort-by-timestamp container as a proxy for "winner" is fragile: Cassandra's reconciliation applies additional tie-breaking (value bytes, deletion status). Any code that bypasses `Cell.reconcile()` and implements its own ordering logic is a candidate for this bug. Review every place that compares or orders cells without calling the canonical reconcile method.

---

## Test

`MaterializedViewLongTest` and `MaterializedViewTest` were updated with additional cases covering the LWW tie-breaking scenario.
