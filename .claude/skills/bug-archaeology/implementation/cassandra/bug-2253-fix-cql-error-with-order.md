# Bug 2253: CQL ORDER BY Fails When Using IN Clause (1.1.x Branch Backport)

**Commit**: 76e092b2d3 · **Ticket**: CASSANDRA-4612
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

This is the 1.1.x branch backport of the same fix applied in commit e681a1a622 (bug-2252). The `ORDER BY` comparators used `ordering.position + 1` — a schema-based static offset — to index into the projected result row. With an `IN` clause on the partition key, rows from multiple partitions are merged and the result columns are indexed by their position in the `SELECT` list, not by schema order. The schema-offset approach read wrong column bytes and produced incorrect sort output.

The fix replaces the static offset with `getColumnPositionInSelect()`, which searches `selectedNames` for the actual projected index, and redesigns `CompositeComparator` to carry an explicit `int[] positions` array rather than a single start index with sequential increment. The 1.1 branch works against `CqlRow` (Thrift layer) rather than `List<ByteBuffer>`, so the diff is structurally similar but adapts `b.getColumns().get(columnPos)` instead of `b.get(columnPos)`.

### Before / After

```java
// BEFORE
Collections.sort(cqlRows,
    new SingleColumnComparator(ordering.position + 1, ordering.type));
```

```java
// AFTER
Collections.sort(cqlRows,
    new SingleColumnComparator(getColumnPositionInSelect(ordering), ordering.type));
```

---

## How to Spot

Same heuristic as bug-2252: whenever sorting a projected query result, column positions must be derived from the actual select-list order, not from schema metadata. Confirm the pattern on every branch that carries the affected code.

---

## Test

No test added.
