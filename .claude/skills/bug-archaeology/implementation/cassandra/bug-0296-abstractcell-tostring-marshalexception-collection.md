# Bug 0296: AbstractCell#toString Throws MarshalException for Tombstone Cell in Collection

**Commit**: a9725b681b · **Ticket**: CASSANDRA-17695
**Subsystem**: src/java/org/apache/cassandra/db/rows/AbstractCell.java
**Category**: Logic error in condition

---

## Root Cause

`AbstractCell#toString` unconditionally called `ct.valueComparator().getString(value(), accessor())` on every cell inside a multi-cell collection, even when the cell was a tombstone. A tombstone cell has no value (the value buffer is empty/null), so invoking the value comparator's deserializer on it threw a `MarshalException`. The fix guards the deserialization path with `isTombstone()` and substitutes the literal string `"<tombstone>"` instead.

### Before / After

```java
// BEFORE
return String.format("[%s[%s]=%s %s]",
                     column().name,
                     ct.nameComparator().getString(path().get(0)),
                     ct.valueComparator().getString(value(), accessor()),
                     livenessInfoString());
```

```java
// AFTER
return String.format("[%s[%s]=%s %s]",
                     column().name,
                     ct.nameComparator().getString(path().get(0)),
                     isTombstone() ? "<tombstone>" : ct.valueComparator().getString(value(), accessor()),
                     livenessInfoString());
```

---

## How to Spot

Whenever `toString` (or any debug/logging method) calls a type-specific deserializer on a cell value, it must first check whether the cell is a tombstone before attempting deserialization, because tombstones carry no value payload. This pattern—calling a serializer without a tombstone guard—is a recurring risk in any diagnostic or logging path that handles the full lifecycle of storage cells.

---

## Test

`CellTest#testUnmarshallableInMulticellCollection` creates a map tombstone cell for every native CQL type and asserts that `cell.toString()` succeeds without throwing `MarshalException`.
