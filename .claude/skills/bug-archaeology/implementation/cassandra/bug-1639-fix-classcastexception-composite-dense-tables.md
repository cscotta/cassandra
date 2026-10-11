# Bug 1639: Fix ClassCastException on Composite Dense Tables

**Commit**: 3aa41fbce8 · **Ticket**: CASSANDRA-7112
**Subsystem**: src/java/org/apache/cassandra/db/composites
**Category**: Logic error in condition

---

## Root Cause

`CompoundDenseCellNameType.makeWith()` decided which concrete type to instantiate based on whether all components were present (`size == fullSize`) and whether an EOC marker was set. When `size < fullSize`, it created a `CompoundComposite` (the generic composite type) and then applied the EOC; when `size == fullSize` and EOC was `NONE`, it created a `CompoundDenseCellName`. The problem is that for backward-compatibility reasons, composite dense cell names do not always have all components set — they can legitimately be partial — so the `size < fullSize` branch was constructing a `CompoundComposite` in situations where callers expected a `CompoundDenseCellName`. Code that later cast the result to `CellName` would then get a `ClassCastException` because `CompoundComposite` does not implement `CellName`.

### Before / After

```java
// BEFORE
if (size < fullSize || eoc != Composite.EOC.NONE)
    return new CompoundComposite(components, size, false).withEOC(eoc);
return new CompoundDenseCellName(components, size);
```

```java
// AFTER
// Always return a CellName; apply EOC on top if needed
Composite c = new CompoundDenseCellName(components, size);
return eoc != Composite.EOC.NONE ? c.withEOC(eoc) : c;
```

---

## How to Spot

When a factory method chooses between a subtype and a supertype based on "completeness" of the data, verify that callers always expect a specific subtype. If any caller casts the result to the subtype, returning the supertype under any condition will cause a `ClassCastException`. The fix is to always return the expected subtype and handle partial state within that subtype.

---

## Test

No test added.
