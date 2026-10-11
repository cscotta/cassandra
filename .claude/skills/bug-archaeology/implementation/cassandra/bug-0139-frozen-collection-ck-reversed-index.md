# Bug 0139: Index Creation Fails for Frozen Collection Clustering Key with DESC Order

**Commit**: 06a05ccde5 · **Ticket**: CASSANDRA-19889
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/schema · src/java/org/apache/cassandra/db/marshal
**Category**: Logic error in condition

---

## Root Cause

`CreateIndexStatement` validates the column type (e.g., checking `isFrozenCollection()`, `isCollection()`, `isMultiCell()`) directly on `column.type`. When a clustering column is declared with `DESC` ordering, Cassandra wraps the underlying type in a `ReversedType<T>` decorator. `ReversedType.isFrozenCollection()` delegates to its inner type, but calling `column.type instanceof MapType` falls through with the outer `ReversedType`, not the actual `MapType`, making the check fail silently and producing a misleading validation error. The fix introduces `AbstractType.unwrap()`, which recursively strips `ReversedType` wrappers, and uses the unwrapped base type for all index validation logic.

### Before / After

```java
// BEFORE – column.type may be ReversedType wrapping the real collection type
if (column.type.isFrozenCollection() && target.type != Type.FULL)
    throw ire("...");
if (!(column.type instanceof MapType && column.type.isMultiCell()) && ...)
    throw ire("...");
```

```java
// AFTER – unwrap reversal before type checks
AbstractType<?> baseType = column.type.unwrap();

if (baseType.isFrozenCollection() && target.type != Type.FULL)
    throw ire("...");
if (!(baseType instanceof MapType && column.type.isMultiCell()) && ...)
    throw ire("...");
```

```java
// NEW helper in AbstractType
public AbstractType<T> unwrap()
{
    return isReversed() ? ((ReversedType<T>) this).baseType.unwrap() : this;
}
```

---

## How to Spot

Any code that interrogates type identity (especially `instanceof` checks or methods like `isFrozenCollection`) on a type that can be wrapped by a decorator such as `ReversedType` is vulnerable to this pattern. Always unwrap decorator layers before structural type checks; relying solely on delegating methods is safe for boolean predicates but not for `instanceof`.

---

## Test

`SecondaryIndexTest.testFullIndexOnClusteringColumn` creates a table with a `frozen<list<int>>` clustering key in `DESC` order, creates a `FULL` index on it, inserts a row, and asserts a `SELECT … WHERE CK=` lookup returns the correct row.
