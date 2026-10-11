# Bug 1624: Fix CellName comparison bugs

**Commit**: 0f977c597a · **Ticket**: CASSANDRA-7227
**Subsystem**: db/composites (AbstractSimpleCellNameType, SimpleCType)
**Category**: Logic error in condition

---

## Root Cause

The comparators in `AbstractSimpleCellNameType` and `SimpleCType` had inverted logic when one or both composites were empty. The ternary condition checked `c1isEmpty` to return `1` (meaning c1 > c2), but an empty composite is the minimum/sentinel value and should compare *less than* any non-empty composite. The correct check is `!c1isEmpty` to return `1` (non-empty c1 is greater than empty c2). Additionally, `SimpleCType` was also missing the return statement for the ByteBuffer empty-check branch entirely, falling through to `compareUnsigned` regardless.

### Before / After

```java
// BEFORE (AbstractSimpleCellNameType & SimpleCType)
if (c1isEmpty || c2isEmpty)
    return c1isEmpty ? 1 : (c2isEmpty ? -1 : 0);
// Returns 1 (c1>c2) when c1 IS empty — backwards
```

```java
// AFTER
if (c1isEmpty || c2isEmpty)
    return !c1isEmpty ? 1 : (!c2isEmpty ? -1 : 0);
// Returns 1 (c1>c2) when c1 is NOT empty — correct
```

---

## How to Spot

When a comparator returns different values for the same pair of objects depending on which is passed first (asymmetry), sort order will be corrupted. Review any comparator that branches on a "sentinel/empty" condition and verify that the sentinel sorts at the correct end of the range.

---

## Test

`CTypeTest` added with three test methods (`testCompoundType`, `testSimpleType1`, `testSimpleType2`) that verify ordering is antisymmetric and that the `Composites.EMPTY` sentinel always sorts below any concrete cell name.
