# Bug 2071: IN Clause Validation Uses Table-Level Collection Presence Instead of Per-Query Selection

**Commit**: 193e56607b · **Ticket**: CASSANDRA-5376
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The CQL3 query planner uses a flag `cfDef.hasCollections` to decide whether a query must be executed as a range scan (rather than a multi-get using an IN clause). The flag is true if the table *definition* contains any collection columns, regardless of whether the query's SELECT list actually reads any of them. This caused two problems: a query like `SELECT non_collection_col FROM t WHERE pk IN (...)` was incorrectly forced into a range scan even though no collection was selected; and conversely, validation did not block `SELECT collection_col FROM t WHERE pk IN (...)`, which is genuinely unsupported because a collection column maps to a non-deterministic set of sub-columns. The fix introduces a `selectACollection()` helper that inspects the actual selected columns, and updates both the range-scan decision and the IN-restriction validator to use it.

### Before / After

```java
// BEFORE – checks table definition, not actual selection
if (cfDef.hasCollections)
    return true;  // always a range query if any collection exists in table

// ...
assert !cfDef.hasCollections;  // wrong assertion

// ...
else if (restriction.eqValues.size() > 1 && i != stmt.columnRestrictions.length - 1)
    throw new InvalidRequestException(...);  // missing check for collection selection
```

```java
// AFTER – checks only what is actually selected
private boolean selectACollection()
{
    if (!cfDef.hasCollections) return false;
    for (CFDefinition.Name name : selection.getColumnsList())
        if (name.type instanceof CollectionType) return true;
    return false;
}

if (selectACollection()) return true;

assert !selectACollection();

else if (restriction.eqValues.size() > 1)
{
    if (i != stmt.columnRestrictions.length - 1)
        throw new InvalidRequestException(...);
    else if (stmt.selectACollection())
        throw new InvalidRequestException("Cannot restrict PRIMARY KEY part ... by IN relation as a collection is selected");
}
```

---

## How to Spot

Whenever a schema-level boolean flag (e.g. `hasCollections`, `hasCounters`) is used to gate query planning logic, check whether the flag is too broad — it may be true even when the current query does not touch those columns. Per-query inspection of the selected columns is the correct granularity for such decisions.

---

## Test

No test added.
