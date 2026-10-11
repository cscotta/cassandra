# Bug 1021: Fix JSON update with prepared statements (3.0 backport)

**Commit**: 2cbd776250 · **Ticket**: CASSANDRA-10631
**Subsystem**: CQL / Prepared Statements
**Category**: State not cleaned up

---

## Root Cause
This is the 3.0 backport of the same fix as commit 31be903a74 (bug 1022). The `Json.PreparedMarker` class stored a parsed JSON column map as instance state (`columnMap`), with a guard comment noting it would only be computed once per column. However, because a single `PreparedMarker` object is shared across multiple executions of the same prepared statement, the cached `columnMap` from a previous execution persisted into the next one. If a new execution provided different bind values, the stale map from the prior execution was used instead. The fix moves JSON parsing and caching into `QueryOptions`, which has per-execution lifetime, removing instance state from `PreparedMarker` entirely.

### Before / After
```java
// BEFORE — columnMap cached on long-lived PreparedMarker, stale across re-executions
public class PreparedMarker {
    private Map<ColumnIdentifier, Term> columnMap;  // persists across executions!

    public void bind(QueryOptions options) {
        if (columnMap != null) return;              // guard only works first time
        columnMap = parseJson(...);
    }
}

// AFTER — parsing delegated to per-execution QueryOptions cache
public Terminal bind(QueryOptions options) throws InvalidRequestException
{
    Term term = options.getJsonColumnValue(marker.bindIndex, column.name, marker.columns);
    return term == null ? null : term.bind(options);
}
// QueryOptions lazily parses+caches JSON per bindIndex within one query execution
```

---

## How to Spot
Instance state on objects that are reused across multiple query executions (prepared statement nodes, term factories, markers) is a common source of stale-data bugs. In review, scrutinize any field on a class that participates in the prepared-statement AST for mutable per-execution state. A `null` check acting as a "compute once" guard is a red flag when the owning object outlives a single execution; the cache should live in `QueryOptions` or another per-execution scope.

---

## Test
`CQLTester` was extended and secondary-index tests were added to verify correct results when the same `INSERT JSON` prepared statement is executed multiple times with different JSON bind values.
