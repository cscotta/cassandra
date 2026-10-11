# Bug 0871: Sparse Tables Incorrectly Marked Dense During Legacy Schema Upgrade

**Commit**: f37b6f145d · **Ticket**: CASSANDRA-11315
**Subsystem**: Schema Migration / LegacySchemaMigrator
**Category**: Logic error in condition

---

## Root Cause
`LegacySchemaMigrator` determined whether a pre-3.0 table was dense by reading the `is_dense` flag from the system schema. A bug fixed in 2.2 (CASSANDRA-11502) had written `is_dense = true` incorrectly for certain sparse (CQL) tables, but that fix was not applied retroactively. When migrating to 3.0, a table with a stale `is_dense = true` was treated as dense and kept its redundant `compact_value` column and extra clustering column, resulting in a corrupted 3.0 table definition. The fix trusts `is_dense = false` (which was never written incorrectly) but recalculates denseness via `calculateIsDense` when the stored value is `true` or absent. If recalculation determines the table is actually sparse, a new `filterOutRedundantRowsForSparse` step removes the `compact_value` column and the excess clustering key column before constructing the column definitions.

### Before / After
```java
// BEFORE
boolean isDense = tableRow.has("is_dense")
                ? tableRow.getBoolean("is_dense")
                : calculateIsDense(rawComparator, columnRows);

// AFTER
Boolean rawIsDense = tableRow.has("is_dense") ? tableRow.getBoolean("is_dense") : null;
boolean isDense;
if (rawIsDense != null && !rawIsDense)
    isDense = false;          // trust stored false
else
    isDense = calculateIsDense(rawComparator, columnRows);  // recalculate for true/null

Iterable<UntypedResultSet.Row> filteredColumnRows = !isDense && (rawIsDense == null || rawIsDense)
    ? filterOutRedundantRowsForSparse(columnRows, isSuper, isCompound)
    : columnRows;
```

---

## How to Spot
When migration code reads a boolean flag from legacy storage to make a structural decision, consider whether that flag could have been written incorrectly by an older version. A safe pattern is to cross-validate with an independent heuristic (`calculateIsDense`) rather than trusting the stored value unconditionally, especially when the stored value was known to be wrong for a class of tables. The symptom is a node that fails to come up (or produces corrupted reads) after a 2.x-to-3.0 upgrade when the cluster contains tables that were created through Thrift.

---

## Test
No test added.
