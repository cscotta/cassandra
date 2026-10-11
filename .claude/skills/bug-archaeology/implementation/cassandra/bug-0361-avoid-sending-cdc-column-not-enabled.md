# Bug 0361: CDC Column Included in Schema Mutations When CDC Disabled

**Commit**: cb370350a7 · **Ticket**: CASSANDRA-16770
**Subsystem**: src/java/org/apache/cassandra/schema/SchemaKeyspace.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When building a schema `PartitionUpdate` in `SchemaKeyspace.makeUpdateForSchema`, the code removed the `cdc` column from the `ColumnFilter` to avoid broadcasting it to pre-CDC nodes, but then used `PartitionUpdate.fromIterator(partition, builder.build())`. The standard `fromIterator` path reads `fetchedColumns()` from the *iterator* itself rather than from the filter, so the carefully constructed filter had no effect and the `cdc` column was still included in the partition. The fix introduces a new `fromIteratorExplicitColumns` factory that passes the filter's `fetchedColumns()` through to the `Holder` builder, and separately fixes the filter builder to use `selectionBuilder()` (query-only) instead of `allColumnsBuilder` (which marks every column as fetched anyway).

### Before / After

```java
// BEFORE — filter is built but iterator still owns its columns
ColumnFilter.Builder builder = ColumnFilter.allColumnsBuilder(partition.metadata());
// ... removes "cdc" from builder ...
return PartitionUpdate.fromIterator(partition, builder.build());
// fromIterator calls build(iterator, ..., iterator.columns()) — ignores the filter's fetchedColumns
```

```java
// AFTER — explicit columns taken from the filter, not the iterator
ColumnFilter.Builder builder = ColumnFilter.selectionBuilder();
// ... removes "cdc" from builder ...
return PartitionUpdate.fromIteratorExplicitColumns(partition, builder.build());
// uses filter.fetchedColumns() so "cdc" is genuinely absent from the Holder
```

---

## How to Spot

Whenever a filtering step is applied to a `ColumnFilter` before constructing a `PartitionUpdate`, verify that the factory method chosen actually honours those filter columns rather than re-deriving them from the underlying iterator. If the iterator itself carries columns (e.g. via `UnfilteredRowIterator.columns()`), any factory that delegates to `iterator.columns()` will silently bypass the filter and include the unwanted column.

---

## Test

No dedicated test added; the fix is exercised by existing schema distribution tests in mixed-version clusters where pre-CDC nodes reject the `cdc` column.
