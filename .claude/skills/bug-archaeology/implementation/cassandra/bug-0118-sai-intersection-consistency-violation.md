# Bug 0118: SAI Intersection Query Returns Wrong Rows When All Indexed Matches Are Repaired and Multiple Non-Indexed Columns Are Filtered

**Commit**: 953ab6cf64 · **Ticket**: CASSANDRA-20189
**Subsystem**: `src/java/org/apache/cassandra/index/sai/plan`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When SAI executes a query in non-strict mode (because all index matches came from repaired data) and the query has predicates on multiple non-indexed columns, `FilterTree` applied the global `BooleanOperator` (AND or OR) to every expression including the non-indexed column filters. With AND semantics and partial rows split across replicas, a row that satisfies either non-indexed predicate but not both would be discarded even though the distributed nature of the data means neither replica has the complete row. The correct behaviour is to union the non-indexed column results (OR) when the indexed results are fully repaired, treating them as complementary evidence rather than independent mandatory gates. The fix adds `Operation.Expressions` to track which columns are unindexed, and `FilterTree` isolates and ORs their results when the non-strict path is taken with multiple unindexed columns.

### Before / After

```java
// BEFORE – isMutableIntersection only counted non-PK expressions > 1; missed static column cases
public boolean isMutableIntersection() {
    return expressions.stream().filter(e -> !e.column.isPrimaryKeyColumn()).count() > 1;
}
```

```java
// AFTER – returns true for any static column expression alongside another, or two+ regular mutable columns
public boolean isMutableIntersection() {
    int count = 0;
    for (Expression e : expressions) {
        if (e.column.isStatic() && expressions.size() > 1)
            return true;
        if (!e.column.isPrimaryKeyColumn())
            if (++count > 1) return true;
    }
    return false;
}
```

---

## How to Spot

When a query planner has two different evaluation strategies (strict / non-strict) and non-indexed predicates are added as extra AND conditions, ask whether the non-indexed predicates are truly independent tests (AND is correct) or complementary evidence across replicas (OR is correct). Split-row scenarios—where different replicas hold different parts of the same logical row—are a recurring source of this class of bug: an AND across replicas yields false negatives, while an OR correctly collects the full row before post-filtering.

---

## Test

`StrictFilteringTest#testPartialUpdatesOnNonIndexedColumnsAfterRepair` and `testPartialUpdateOnNonIndexedColumnAfterRepair` simulate post-repair split rows on two nodes and assert that a query with both an indexed and non-indexed predicate returns the expected merged result.
