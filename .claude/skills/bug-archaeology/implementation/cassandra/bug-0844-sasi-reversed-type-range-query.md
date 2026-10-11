# Bug 0844: SASI range queries wrong on reversed-type columns

**Commit**: 55a45b58d6 · **Ticket**: CASSANDRA-12223
**Subsystem**: Secondary Indexes / SASI
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`Expression.add()` maps CQL comparison operators (`LT`, `LTE`, `GT`, `GTE`) to lower/upper bounds for the SASI on-disk index search. For a normal ascending type, `LT`/`LTE` constrains the upper bound and `GT`/`GTE` constrains the lower bound. For a `ReversedType`, the physical ordering of bytes in the index is inverted: what the user calls "less than X" is stored at a position after X in the index. The code applied the same bound mapping regardless of type direction, so a query `col < 5` on a reversed column was searching the wrong half of the index, returning either no rows or rows outside the intended range.

### Before / After
```java
// BEFORE
case LTE:
    upperInclusive = true;
case LT:
    operation = Op.RANGE;
    upper = new Bound(value, upperInclusive);
    break;

case GTE:
    lowerInclusive = true;
case GT:
    operation = Op.RANGE;
    lower = new Bound(value, lowerInclusive);
    break;

// AFTER
case LTE:
    if (index.getDefinition().isReversedType())
        lowerInclusive = true;
    else
        upperInclusive = true;
case LT:
    operation = Op.RANGE;
    if (index.getDefinition().isReversedType())
        lower = new Bound(value, lowerInclusive);
    else
        upper = new Bound(value, upperInclusive);
    break;

case GTE:
    if (index.getDefinition().isReversedType())
        upperInclusive = true;
    else
        lowerInclusive = true;
case GT:
    operation = Op.RANGE;
    if (index.getDefinition().isReversedType())
        upper = new Bound(value, upperInclusive);
    else
        lower = new Bound(value, lowerInclusive);
    break;
```

---

## How to Spot
Any range-operator mapping in an index search layer must account for `ReversedType`. The tell-tale sign is that the mapping is written without a call to `isReversedType()`. In test coverage, always include a `WITH CLUSTERING ORDER BY (col DESC)` variant of range queries; a bug of this kind will cause all range predicates on the reversed column to return empty or wrong result sets.

---

## Test
`SASIIndexTest` was extended with a test that creates a table with a reversed clustering column, inserts rows, and verifies that `<`, `<=`, `>`, and `>=` queries return the correct rows.
