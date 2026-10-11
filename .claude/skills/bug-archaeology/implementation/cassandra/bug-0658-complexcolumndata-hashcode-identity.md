# Bug 0658: ComplexColumnData hashCode Uses Array Identity Instead of Content

**Commit**: 1e80e35396 · **Ticket**: CASSANDRA-13263
**Subsystem**: src/java/org/apache/cassandra/db/rows/ComplexColumnData.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ComplexColumnData.hashCode()` passed the raw `cells` BTree array reference to `Objects.hash()`. Because `Objects.hash()` defers to the object's own `hashCode()`, and arrays in Java compute identity-based hashes rather than content-based hashes, two `ComplexColumnData` instances holding identical cells would produce different hash codes if the arrays were different objects. This violated the `hashCode`/`equals` contract and could cause silent failures in hash-based data structures (e.g., sets and maps keyed by `ColumnData`).

### Before / After

```java
// BEFORE
@Override
public int hashCode()
{
    return Objects.hash(column(), complexDeletion(), cells);
}
```

```java
// AFTER
@Override
public int hashCode()
{
    return Objects.hash(column(), complexDeletion(), BTree.hashCode(cells));
}
```

---

## How to Spot

Whenever a class aggregates an array (or any non-primitive container) and overrides `hashCode` via `Objects.hash(...)`, reviewers must check that the container's *content* hash rather than its *identity* hash is passed. Arrays require `Arrays.hashCode()` (or, in BTree-backed structures, the dedicated `BTree.hashCode()` helper). The same applies to `equals`: if the class already uses structural equality, the matching `hashCode` must also be structural.

---

## Test

No test added.
