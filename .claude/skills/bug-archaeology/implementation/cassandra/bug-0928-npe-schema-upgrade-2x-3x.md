# Bug 0928: NPE on Schema Upgrade from 2.x to 3.x

**Commit**: 1a1f836f20 · **Ticket**: CASSANDRA-10943
**Subsystem**: Schema migration
**Category**: Missing null / bounds check

---

## Root Cause
When migrating schema from Cassandra 2.x to 3.x, `LegacySchemaMigrator` called `tableRow.getBoolean("is_dense")` unconditionally, but the `is_dense` column did not exist in the legacy system schema for tables that were created through the Thrift API or before CASSANDRA-7744. The `UntypedResultSet.Row.getBoolean()` method returned null / threw an NPE when the column was absent from the row. The fix adds an existence check (`tableRow.has("is_dense")`) and falls back to inferring the dense flag from the comparator type and column definitions via a new `calculateIsDense()` method.

### Before / After
```java
// BEFORE
boolean isDense = tableRow.getBoolean("is_dense");

// AFTER
boolean isDense = tableRow.has("is_dense")
                ? tableRow.getBoolean("is_dense")
                : calculateIsDense(rawComparator, columnRows);
```

---

## How to Spot
Any place that reads a column from a legacy system table row without a prior `has()` guard is a candidate for this class of NPE. The risk is highest in migration code paths that must handle schema rows written by older Cassandra versions where columns were added in later releases. During review, check every `row.getXxx(columnName)` call in `LegacySchemaMigrator` and similar migration classes to ensure a `row.has(columnName)` guard or a default value is provided.

---

## Test
No dedicated test added for this specific NPE path. The fix is guarded by the broader schema-migration test suite.
