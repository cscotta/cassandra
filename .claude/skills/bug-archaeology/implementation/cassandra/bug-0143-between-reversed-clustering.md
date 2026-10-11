# Bug 0143: BETWEEN Operator Returns Wrong Results on Reversed Clustering Columns

**Commit**: c7e9048d4d · **Ticket**: CASSANDRA-19878
**Subsystem**: src/java/org/apache/cassandra/cql3/Operator.java · src/java/org/apache/cassandra/cql3/restrictions/SimpleRestriction.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The in-memory `BETWEEN` satisfaction check in `Operator.BETWEEN.isSatisfiedBy()` sorted the two bound values with `type` (the column's natural comparator) and compared using `compareForCQL`. For reversed clustering columns the comparator is a `ReversedType`, so `compareForCQL` — which ignores reversal and compares by raw bytes in ascending order — produces the opposite ordering from what the storage engine sees, causing the range check to return incorrect results. Two separate issues were fixed:

1. The bounds sort inside `isSatisfiedBy` was removed (the caller now handles it).
2. `compare` (which respects reversal) replaced `compareForCQL` (which does not) in the bounds check.
3. `SimpleRestriction` was updated to sort the user-supplied bounds using `column.type` before packing them into the filter expression, implementing SQL-style reversed-bounds tolerance.

### Before / After

```java
// BEFORE – in Operator.BETWEEN.isSatisfiedBy()
List<ByteBuffer> buffers = ListType.getInstance(type, false).unpack(rightOperand);
buffers.sort(type);   // sort done here, using potentially reversed comparator
return type.compareForCQL(leftOperand, buffers.get(0)) >= 0   // ignores reversal
    && type.compareForCQL(leftOperand, buffers.get(1)) <= 0;
```

```java
// AFTER – sort delegated to caller; use compare() to respect reversal
List<ByteBuffer> buffers = ListType.getInstance(type, false).unpack(rightOperand);
// No sort here
return type.compare(leftOperand, buffers.get(0)) >= 0   // respects ReversedType
    && type.compare(leftOperand, buffers.get(1)) <= 0;
```

```java
// AFTER – SimpleRestriction sorts bounds before packing
if (operator.kind() == Operator.Kind.TERNARY)
    buffers.sort(column.type);   // ensures lo <= hi for the physical comparator
filter.add(column, operator, multiInputOperatorValues(column, buffers));
```

---

## How to Spot

Any range or interval comparison on a Cassandra column must use `AbstractType.compare()` rather than `compareForCQL()` if the column could be a reversed clustering key. `compareForCQL` is designed for cross-type CQL semantics and strips reversal; `compare` preserves it. Similarly, sorting user-supplied range bounds must use the physical comparator, not a hard-coded ascending order.

---

## Test

`SelectSingleColumnRelationTest.testBetweenFilteringWithReversedOrdering` creates tables both with and without `CLUSTERING ORDER BY ... DESC`, inserts four rows, and asserts that `BETWEEN 2 AND 3` and the reversed `BETWEEN 3 AND 2` both return the same correct two rows under each table ordering.
