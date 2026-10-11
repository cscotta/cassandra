# Bug 2034: CQL3 Function Results Rejected for Descending Clustering Columns Due to Type Mismatch

**Commit**: 7eae57aeac · **Ticket**: CASSANDRA-5472
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/Selection.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SimpleSelector.isAssignableTo()` and `FunctionSelector.isAssignableTo()` compared `AbstractType` instances using raw `equals()`. For descending clustering columns, Cassandra wraps the column's type in a `ReversedType` decorator, so `ReversedType(IntegerType)` did not equal `IntegerType` even though they represent the same CQL3 type. The fix compares `asCQL3Type()` representations instead, which strips the reversal decorator before comparing.

### Before / After

```java
// BEFORE
public boolean isAssignableTo(ColumnSpecification receiver)
{
    return type.equals(receiver.type);
}
```

```java
// AFTER
public boolean isAssignableTo(ColumnSpecification receiver)
{
    return type.asCQL3Type().equals(receiver.type.asCQL3Type());
}
```

---

## How to Spot

Wherever Cassandra compares `AbstractType` instances for equality in the context of CQL (as opposed to storage-level comparisons), the comparison should go through `asCQL3Type()` to avoid false mismatches caused by `ReversedType` or other decorator wrappers.

---

## Test

No test added.
