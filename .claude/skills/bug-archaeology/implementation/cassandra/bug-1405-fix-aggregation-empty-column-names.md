# Bug 1405: Fix aggregation fn on empty selection, column names

**Commit**: 4ff6e7dfff · **Ticket**: CASSANDRA-8229
**Subsystem**: src/java/org/apache/cassandra/cql3/selection/Selection.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Two distinct but related bugs existed in the CQL3 selection/aggregation layer. First, aggregate functions (COUNT, MAX, MIN, SUM, AVG) on an empty table returned zero rows instead of one row containing the aggregate identity values (e.g., `COUNT(*) = 0`). The `ResultSetBuilder.build()` method finished the loop and returned an empty result set without checking whether an aggregate-only selection should emit one output row. Second, each `Selector.Factory` subclass redundantly constructed a full `ColumnSpecification` object including keyspace and table name, duplicating that logic across every implementation. The column name embedded in these specs was derived from `fun.toString()` rather than a properly formatted `functionName(arg1, arg2)` string, producing mismatched column names. The fix extracted a `getColumnName()` / `getReturnType()` template-method pair into the base `Factory` class, centralizing `ColumnSpecification` construction and ensuring uniform naming.

### Before / After

```java
// BEFORE — no row emitted for aggregates on empty table
public ResultSet build(int protocolVersion) throws InvalidRequestException
{
    if (current != null) { ... }
    selectors.reset();
    current = null;
    return resultSet;   // empty when table has no rows
}

// Column name from function selector:
new ColumnIdentifier(fun.toString(), true)  // e.g. "org.apache.cassandra.db.functions.CountRows"
```

```java
// AFTER
public ResultSet build(int protocolVersion) throws InvalidRequestException
{
    if (current != null) { ... }
    selectors.reset();
    current = null;
    if (resultSet.isEmpty() && selectors.isAggregate())
        resultSet.addRow(selectors.getOutputRow());
    return resultSet;
}

// Column name from AbstractFunctionSelector factory:
protected String getColumnName() {
    return new StrBuilder(fun.name().toString())
            .append('(').appendWithSeparators(factories.getColumnNames(), ", ").append(')')
            .toString();   // e.g. "count(*)", "max(b)"
}
```

---

## How to Spot

Aggregate functions have a well-known SQL semantic: even on an empty table they return exactly one row. Any code path that simply collects rows in a loop and returns the accumulated set without a post-loop check for the aggregate-on-empty case violates this contract. Reviewers should look for `ResultSetBuilder.build()` or equivalent result-collection methods and ask: "if no rows were processed, is the aggregate identity value still emitted?" Similarly, when multiple subclasses each construct the same compound object, extracting a template method prevents divergent implementations and the subtle naming bugs they introduce.

---

## Test

`AggregationTest.testFunctions` extended with empty-table assertions for COUNT, MAX, MIN, SUM, AVG; new `testFunctionsWithCompactStorage` added; column name assertions added via new `assertColumnNames` helper in `CQLTester`.
