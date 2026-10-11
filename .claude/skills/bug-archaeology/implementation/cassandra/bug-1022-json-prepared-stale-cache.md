# Bug 1022: Fix JSON update with prepared statements

**Commit**: 31be903a74 · **Ticket**: CASSANDRA-10631
**Subsystem**: CQL / Prepared Statements
**Category**: State not cleaned up

---

## Root Cause
`Json.PreparedMarker` cached the parsed JSON column map as an instance field (`columnMap`), intending to parse the JSON string only once per column despite `bind()` being called once per column in the statement. Since a `PreparedMarker` is part of the prepared-statement AST which is reused across executions, `columnMap` survived from one execution to the next. On a subsequent execution with new bind values, the early-exit guard (`if (columnMap != null) return;`) silently returned the stale map from the previous execution, causing the new bind values to be ignored. The fix eliminates the mutable cache from `PreparedMarker` and moves JSON parsing into `QueryOptions.getJsonColumnValue()`, which exists for the lifetime of a single query execution and lazily populates a per-bindIndex cache.

### Before / After
```java
// BEFORE — mutable state on reused PreparedMarker
public class PreparedMarker {
    private Map<ColumnIdentifier, Term> columnMap;  // reused across executions

    public void bind(QueryOptions options) {
        if (columnMap != null) return;              // wrong: stale on re-execution
        ByteBuffer value = options.getValues().get(bindIndex);
        columnMap = parseJson(UTF8Type.instance.getSerializer().deserialize(value), columns);
    }
}

// AFTER — cache lives in per-execution QueryOptions
// QueryOptions.getJsonColumnValue():
public Term getJsonColumnValue(int bindIndex, ColumnIdentifier columnName,
                               Collection<ColumnDefinition> expectedReceivers) {
    if (jsonValuesCache == null)
        jsonValuesCache = new ArrayList<>(Collections.nCopies(getValues().size(), null));
    Map<ColumnIdentifier, Term> jsonValue = jsonValuesCache.get(bindIndex);
    if (jsonValue == null) {
        ByteBuffer value = getValues().get(bindIndex);
        jsonValue = Json.parseJson(UTF8Type.instance.getSerializer().deserialize(value), expectedReceivers);
        jsonValuesCache.set(bindIndex, jsonValue);
    }
    return jsonValue.get(columnName);
}
```

---

## How to Spot
Prepared-statement AST nodes (terms, markers, restrictions) are long-lived objects shared across query executions. Any mutable field on these objects that is intended to be "per-execution" must be moved to `QueryOptions` or another execution-scoped object. In review, ask whether a field is populated during `bind(QueryOptions)` — if so, it must not persist beyond that single `QueryOptions` instance. The `onUpdateFunction`/`onUpdateAggregate` handlers also added in this commit fix a related gap where updating a UDF/aggregate did not invalidate prepared statements referencing the old definition.

---

## Test
`CQLTester` was extended to verify that executing the same `INSERT JSON` prepared statement twice with different JSON values produces two distinct correct results.
