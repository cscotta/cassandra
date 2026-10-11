# Bug 2252: CQL ORDER BY Fails When Using IN Clause Due to Wrong Column Position Calculation

**Commit**: e681a1a622 · **Ticket**: CASSANDRA-4612
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `ORDER BY` comparators (`SingleColumnComparator` and `CompositeComparator`) calculated the position of an ordering column in the result set using `ordering.position + 1` — a fixed offset based on the column's schema position. When an `IN` clause is used on the partition key, the result set is assembled from multiple partitions and the column positions in the projected row no longer correspond to schema-definition order. This caused the comparator to read the wrong column's bytes and produce incorrect or error-prone sort results.

The fix introduces `getColumnPositionInSelect(CFDefinition.Name)` which scans `selectedNames` to find the actual index of the ordering column in the projected output. The multi-column `CompositeComparator` was also changed from a `startIndex + sequential increment` design to an explicit `int[] positions` array, so each ordering column is looked up independently regardless of whether the columns are contiguous in the result.

### Before / After

```java
// BEFORE (single-column)
Collections.sort(cqlRows.rows,
    new SingleColumnComparator(ordering.position + 1, ordering.type));

// BEFORE (multi-column)
int startPosition = orderingColumn.position + 1;  // first ordering col only
Collections.sort(cqlRows.rows, new CompositeComparator(startPosition, types));
// comparator increments currentIndex sequentially — wrong if cols non-contiguous
```

```java
// AFTER (single-column)
Collections.sort(cqlRows.rows,
    new SingleColumnComparator(getColumnPositionInSelect(ordering), ordering.type));

// AFTER (multi-column)
int[] positions = new int[parameters.orderings.size()];
positions[idx++] = getColumnPositionInSelect(orderingColumn);
Collections.sort(cqlRows.rows, new CompositeComparator(types, positions));
```

---

## How to Spot

When sorting projected query results, always derive column positions from the projected output (`selectedNames`) rather than from schema metadata positions. Schema positions are stable within a table definition but do not account for non-wildcard `SELECT` lists, `IN` multi-partition queries, or other result-set reordering.

---

## Test

No test added.
