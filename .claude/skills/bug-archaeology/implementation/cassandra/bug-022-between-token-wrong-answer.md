# Bug 022: BETWEEN where token(Y) > token(Z) returns wrong answer

**Commit**: bad0a10343 · **Ticket**: CASSANDRA-20154
**Subsystem**: CQL query processing / restrictions
**Category**: Wrong constant / default value

---

## Root Cause

The `BETWEEN` operator was designed to follow standard SQL semantics: when the lower bound is greater than the upper bound the result should be empty. To achieve this, `SimpleRestriction` sorted the two bound values before filtering — but this sort was applied unconditionally, including to `token()` restrictions. Token ordering is circular and not directly comparable via the column type comparator, so sorting token bounds silently inverted inverted-range token queries (e.g., `token(id) BETWEEN 0 AND MIN_TOKEN` was treated the same as the reversed form). Additionally, `Operator.BETWEEN.isSatisfiedBy` called `type.compare` on a wrapped (e.g., reversed) type instead of first unwrapping it, so the comparison used the reversed-column ordering rather than the natural value order.

### Before / After

```java
// BEFORE — sorts BETWEEN bounds even for token() restrictions
if (operator.kind() == Operator.Kind.TERNARY)
    buffers.sort(column.type);   // wrong: sorts token bounds too

// BEFORE — isSatisfiedBy compares against wrapped (possibly reversed) type
return type.compare(leftOperand, buffers.get(0)) >= 0
    && type.compare(leftOperand, buffers.get(1)) <= 0;

// AFTER — only sort IN lists; BETWEEN bounds are left in user-supplied order
if (operator == Operator.IN && !column.type.isCounter())
    buffers.sort(column.type);   // BETWEEN is no longer sorted here

// AFTER — isSatisfiedBy unwraps before comparing
AbstractType<?> unwrapped = type.unwrap();
return unwrapped.compare(leftOperand, buffers.get(0)) >= 0
    && unwrapped.compare(leftOperand, buffers.get(1)) <= 0;

// AFTER — restrict() no longer sorts args either; inverted bounds yield empty
rangeSet.removeAll(ClusteringElements.lessThan(args.get(0), partitioner));
rangeSet.removeAll(ClusteringElements.greaterThan(args.get(1), partitioner));
```

---

## How to Spot

Sorting operator operands "for convenience" before applying range logic is a red flag whenever the operator is supposed to respect user-supplied order (SQL `BETWEEN` with inverted bounds must return empty, not swap them). A reviewer should ask: "Does this sort change the observable semantics for any data type?" — special-cased token types and reversed clustering columns are common sources of such surprises. Similarly, using a wrapped type in a comparison without calling `unwrap()` is a latent bug for any reversed column.

---

## Test

`BetweenInversionTest.testBetweenInversion` inserts two rows and asserts that `BETWEEN 1 AND 2` returns 2 rows while `BETWEEN 2 AND 1` (inverted) returns 0 rows.
