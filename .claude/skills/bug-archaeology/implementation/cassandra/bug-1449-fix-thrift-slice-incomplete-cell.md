# Bug 1449: Thrift slice deletions and incomplete cell names in updates

**Commit**: 071b5f8321 · **Ticket**: CASSANDRA-7990
**Subsystem**: src/java/org/apache/cassandra/db/composites/CompoundDenseCellNameType.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CompoundDenseCellNameType.create()` contained two incorrect assertions: it assumed the prefix always had exactly `fullSize` components and that the `CompoundComposite`'s element array was fully populated (`lc.elements.length == lc.size`). In COMPACT STORAGE tables it is legal for a cell name to omit trailing clustering columns, so partial prefixes are valid. Separately, `CassandraServer` called `cellFromByteBuffer()` for Thrift slice range endpoints, which unconditionally casts the result to `CellName`; for partial composite keys the deserialized composite is not a `CellName`, causing a `ClassCastException` at runtime. The fix is to relax the assertions and use the more general `fromByteBuffer()` for range endpoints, which correctly returns a `Composite` (not necessarily a `CellName`).

### Before / After

```java
// BEFORE — asserts full-size prefix, loses actual size
assert prefix.size() == fullSize;
...
assert lc.elements.length == lc.size;
return new CompoundDenseCellName(lc.elements);
```

```java
// AFTER — tolerates partial composite, preserves actual size
// assertion removed; lc.size carries the real component count
return new CompoundDenseCellName(lc.elements, lc.size);
```

```java
// BEFORE — CassandraServer: unsafe cast for slice range endpoints
cfm.comparator.cellFromByteBuffer(del.predicate.getSlice_range().start)

// AFTER — use the safe, general-purpose conversion
cfm.comparator.fromByteBuffer(del.predicate.getSlice_range().start)
```

---

## How to Spot

Any assertion of the form "size equals capacity" or "length equals logical count" on composite/tuple structures is suspicious when the schema allows variable-length clustering keys. Review every call site of `cellFromByteBuffer()` to confirm the caller can truly guarantee a full `CellName` rather than a partial `Composite`; if there is any doubt, prefer the weaker `fromByteBuffer()` and let the caller handle the type check.

---

## Test

No test added.
