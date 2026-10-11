# Bug 0598: SELECT JSON with IN Restriction and ORDER BY Returns Wrong Results

**Commit**: 594cde7937 · **Ticket**: CASSANDRA-14286
**Subsystem**: `org.apache.cassandra.cql3.selection`, `org.apache.cassandra.cql3.statements`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

For multi-partition queries (`IN` restriction on the partition key) with an `ORDER BY` clause, Cassandra performs a post-query sort. The sort comparator is built using column index positions in the result row. When `SELECT JSON` is active, all selected columns are collapsed into a single JSON string in position 0, so the original column-position indices are meaningless. The ordering columns must still be fetched and appended after the JSON column for sorting, but the code did not do this: `getResultMetadata(isJson)` returned metadata with only the JSON column, the ordering-column indices pointed into the raw (pre-JSON) row layout, and `rowToJson` iterated over `metadata.names.size()` (the full column list including ordering extras) instead of just `columnCount` (the columns the user requested), corrupting the JSON output.

### Before / After

```java
// BEFORE – ordering index used raw column positions, no ordering cols in JSON row
return new ResultSet.ResultMetadata(Arrays.asList(jsonSpec));
// rowToJson iterated all of metadata.names including ordering extras
for (int i = 0; i < metadata.names.size(); i++) { ... }
```

```java
// AFTER – ordering cols appended after JSON col; indices remapped to post-JSON positions
ResultSet.ResultMetadata resultMetadata = new ResultSet.ResultMetadata(Lists.newArrayList(jsonSpec));
if (orderingIndex != null)
    for (ColumnDefinition c : orderingIndex.keySet())
        resultMetadata.addNonSerializedColumn(c);
// rowToJson only iterates user-visible columns
for (int i = 0; i < metadata.getColumnCount(); i++) { ... }
// Ordering columns appended to jsonRow for sort comparator use
for (Integer idx : orderingIndex.values())
    jsonRow.add(outputRow.get(idx));
```

---

## How to Spot

Whenever a query transformation (JSON serialization, column aliasing, wildcard expansion) changes the number or order of columns in a result row, every downstream component that addresses columns by numeric index must be re-examined. A reviewer should search for index lookups derived from the pre-transformation column list and verify they are still valid — or remapped — in the post-transformation layout.

---

## Test

`JsonTest.testJsonOrdering` inserts two rows and verifies `SELECT JSON … WHERE a IN (…) ORDER BY b` and `ORDER BY b DESC` return rows in the correct order, including when the ordering column is not in the SELECT list.
