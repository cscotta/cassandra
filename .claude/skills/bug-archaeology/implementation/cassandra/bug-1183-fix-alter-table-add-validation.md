# Bug 1183: Fix ALTER TABLE ADD Validation for Frozen Collections

**Commit**: fa7103ffdd · **Ticket**: CASSANDRA-9816
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java
**Category**: Logic error in condition

---

## Root Cause

`AlterTableStatement` prevented re-adding a collection column whose name had been used before if the new type was incompatible with the dropped type. The intent was to guard against stale cells from a non-frozen (multi-cell) dropped collection being misinterpreted by a new collection of a different type. However, the check fired even when the previously dropped column was a frozen (single-cell) collection. Frozen collections are stored as a single opaque blob and carry no per-element cell history, so there is no shadowing risk. The missing `dropped.type.isMultiCell()` guard caused `ALTER TABLE ADD` to be rejected incorrectly whenever the old dropped column happened to be a frozen collection of a different type.

### Before / After

```java
// BEFORE
if (dropped != null && dropped.type instanceof CollectionType && !type.isCompatibleWith(dropped.type))
    throw new InvalidRequestException(...);
```

```java
// AFTER
if (dropped != null && dropped.type instanceof CollectionType
    && dropped.type.isMultiCell() && !type.isCompatibleWith(dropped.type))
{
    throw new InvalidRequestException(...);
}
```

---

## How to Spot

Compatibility guards that treat all members of a type hierarchy (here, all `CollectionType` instances) identically can be too conservative when subsets of that hierarchy have meaningfully different storage semantics. Whenever a collection-type check was written before frozen collections existed, it likely needs a `isMultiCell()` discriminator added. Reviewers should ask: does this invariant apply to both frozen and non-frozen variants, or only to the multi-cell case?

---

## Test

`CollectionsTest` is extended with a case that drops a frozen collection column and then re-adds a column of the same name with a different frozen collection type, verifying that the `ALTER TABLE ADD` succeeds.
