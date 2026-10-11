# Bug 0608: Wildcard GROUP BY Queries Return Wrong Results

**Commit**: 515f07b5ac · **Ticket**: CASSANDRA-14209
**Subsystem**: `src/java/org/apache/cassandra/cql3/selection/Selection.java`, `cql3/statements/SelectStatement.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a `SELECT *` query included a `GROUP BY` clause, the code path fell through to `Selection.wildcard()`, which creates a `SimpleSelection` without processing columns through `fromSelectors`. The `fromSelectors` path is responsible for attaching group-by-aware aggregation metadata to each selector; skipping it meant that aggregation context was never established for wildcard columns. The fix introduces a dedicated `wildcardWithGroupBy()` factory that enumerates every column as a `RawSelector` and routes them through `fromSelectors` exactly like an explicit column list would.

### Before / After

```java
// BEFORE (SelectStatement.java)
Selection selection = selectClause.isEmpty()
    ? Selection.wildcard(cfm)
    : Selection.fromSelectors(cfm, selectClause, boundNames,
                              !parameters.groups.isEmpty());
```

```java
// AFTER
private Selection prepareSelection(CFMetaData cfm, VariableSpecifications boundNames) {
    boolean hasGroupBy = !parameters.groups.isEmpty();
    if (selectClause.isEmpty())
        return hasGroupBy
            ? Selection.wildcardWithGroupBy(cfm, boundNames)
            : Selection.wildcard(cfm);
    return Selection.fromSelectors(cfm, selectClause, boundNames, hasGroupBy);
}
```

---

## How to Spot

Any time two code paths (wildcard vs. explicit selector) diverge at query preparation and one of them simply ignores a query modifier, look for cases where the ignored modifier affects result shape. A reviewer should ask: "Does this modifier change how individual columns are processed during execution?" If yes, the wildcard shortcut must go through the same preparation pipeline as the explicit path.

---

## Test

`SelectGroupByTest.testGroupByWithRangeNamesQueryOnPartitionWithoutRows` and numerous new wildcard-plus-GROUP-BY cases added to `SelectGroupByTest` and `JsonTest` assert that `SELECT * FROM t GROUP BY k` returns one row per group with the first row's values, across single-partition, range, multi-partition, paged, and JSON variants.
