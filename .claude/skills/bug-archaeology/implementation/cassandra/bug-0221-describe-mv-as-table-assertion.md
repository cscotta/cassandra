# Bug 0221: DESCRIBE TABLE on a Materialized View Name Causes AssertionError

**Commit**: 92d0a40c96 · **Ticket**: CASSANDRA-18596
**Subsystem**: cql3/statements (DescribeStatement, KeyspaceMetadata)
**Category**: Missing null / bounds check

---

## Root Cause

`DescribeStatement`'s table-describing element used `ks.getTableOrViewNullable(t)` to look up the table, then passed the result to `table.indexes` — which is not valid for a `ViewMetadata`-backed `TableMetadata` because a materialized view cannot have secondary indexes. When the user ran `DESCRIBE TABLE <mv_name>`, the lookup succeeded (returning the view's underlying `TableMetadata`), but the subsequent `table.indexes.stream()` call triggered an assertion failure. The fix introduces `KeyspaceMetadata.getTableNullable()` that looks up only in the `tables` map (not views), so describing a view by its name correctly returns a `null` and the `checkNotNull()` guard raises a friendly `"Table '...' not found"` error instead.

### Before / After

```java
// BEFORE – also matches materialized views, causing assertion failure downstream
TableMetadata table = checkNotNull(ks.getTableOrViewNullable(t),
                                   "Table '%s' not found in keyspace '%s'", t, ks.name);
```

```java
// AFTER – only matches base tables, views produce a clean error message
TableMetadata table = checkNotNull(ks.getTableNullable(t),
                                   "Table '%s' not found in keyspace '%s'", t, ks.name);
```

---

## How to Spot

When a lookup method intentionally conflates two different schema object types (tables and views), callers that expect only one type must use a narrower accessor. Review every call site of `getTableOrViewNullable` and verify that the caller is prepared to handle both `TableMetadata` from a base table and the flavour returned for a view; if it is not, use the type-specific accessor. A missing-or-wrong schema-object kind check before calling methods that are only valid for base tables is a reliable indicator of this class of bug.

---

## Test

`DescribeStatementTest.testDescribeMaterializedViewAsTable` creates a materialized view and asserts that `DESCRIBE TABLE <mv_name>` raises `InvalidRequestException` with the message `"Table '...' not found in keyspace '...'"` rather than an `AssertionError`.
