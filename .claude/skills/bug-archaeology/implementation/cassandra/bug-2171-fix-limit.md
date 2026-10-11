# Bug 2171: CQL3 LIMIT Counted Internal Columns Instead of CQL Rows

**Commit**: a32eb9f7d2 · **Ticket**: CASSANDRA-4877
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/db/filter/
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The CQL3 `SELECT … LIMIT N` clause was supposed to limit the number of logical CQL rows returned. However, the internal `RangeSliceCommand` used a `maxIsColumns` flag that was set to `true` only when the filter was a `SliceQueryFilter`, and `false` for `NamesQueryFilter`. For `NamesQueryFilter`, the limit was interpreted as a count of internal Thrift rows rather than CQL rows. Additionally, `Row.getLiveColumnCount()` counted raw internal columns rather than CQL-level rows, so pagination and short-read protection were also broken. The fix always sets `countCQL3Rows=true` for CQL3 range queries and introduces a `getLiveCount(filter)` method on `Row` that delegates to the filter's own row-counting logic.

### Before / After

```java
// BEFORE — NamesQueryFilter bypassed CQL3 row counting
boolean maxIsColumns = filter instanceof SliceQueryFilter;
return new RangeSliceCommand(..., getLimit(), maxIsColumns, false);
```

```java
// AFTER — always count CQL3 rows for CQL3 range queries
// We want to have getRangeSlice to count the number of columns, not the number of keys.
return new RangeSliceCommand(..., getLimit(), true, false);
```

---

## How to Spot

When a query layer that models rows differently from the storage layer adds a LIMIT feature, verify that every code path that evaluates the limit count is measuring the logical unit (CQL row) and not the physical unit (internal column). Watch for boolean flags that toggle between two counting modes — missing coverage on one branch will cause incorrect limits.

---

## Test

Existing `ColumnFamilyStoreTest` updated to use the new `getLiveCount(filter)` API.
