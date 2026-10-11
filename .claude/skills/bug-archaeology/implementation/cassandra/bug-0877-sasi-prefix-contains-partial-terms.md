# Bug 0877: SASI PREFIX Search in CONTAINS Mode Skips Partial Terms Incorrectly

**Commit**: 7107646ace · **Ticket**: CASSANDRA-12073
**Subsystem**: SASI secondary index / on-disk index
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a SASI index column is configured with `mode=CONTAINS` (analyzer splits values into tokens) and a PREFIX query is issued, the on-disk index stores both full tokens and "partial" term markers for prefix matching. The `Expression.isLowerSatisfiedBy` and `isUpperSatisfiedBy` methods both called a helper `nonMatchingPartial(term)` which returned `true` for any partial term under a PREFIX operation, causing such terms to be unconditionally rejected before the range comparison. This meant prefix-matching terms that should have been emitted as query results were silently filtered out. The fix moves the partial-term skip logic into `OnDiskIndex`'s iterator, which steps over partial terms during forward iteration, and removes the premature rejection from the boundary-check methods so that partial terms can correctly satisfy the lower/upper bounds when the iterator reaches them.

### Before / After
```java
// BEFORE — partial terms unconditionally rejected in bound checks
public boolean isLowerSatisfiedBy(OnDiskIndex.DataTerm term) {
    if (!hasLower()) return true;
    if (nonMatchingPartial(term)) return false;  // kills valid prefix matches
    ...
}
private boolean nonMatchingPartial(OnDiskIndex.DataTerm term) {
    return term.isPartial() && operation == Op.PREFIX;
}

// AFTER — skip partial terms in the iterator; bound checks are unconditional
// In OnDiskIndex iterator:
if (e.getOp() == Op.PREFIX && currentTerm.isPartial())
    continue;  // step over partial terms while scanning
// nonMatchingPartial() removed; isLowerSatisfiedBy/isUpperSatisfiedBy
// no longer call it.
```

---

## How to Spot
When a filtering predicate lives in a bounds-check method that is called per-term, but the correct semantic is "skip during iteration", the result is that valid terms are rejected at the wrong layer. In index iterators, partial-match skipping belongs in the loop's `continue` guard, not in the boundary satisfaction predicate that determines when to stop.

---

## Test
`SASIIndexTest.testPrefixSearchWithContainsMode` and `OnDiskIndexTest` were extended with new test cases covering prefix queries on CONTAINS-mode indexes with and without flushed SSTables.
