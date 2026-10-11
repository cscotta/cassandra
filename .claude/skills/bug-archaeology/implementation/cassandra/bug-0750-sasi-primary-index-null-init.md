# Bug 0750: SASI primary index never initialized when expression is null

**Commit**: a2dffc2b49 · **Ticket**: CASSANDRA-12910
**Subsystem**: Secondary indexes (SASI)
**Category**: Logic error in condition

---

## Root Cause
`QueryController.getIndexes()` selects the "primary" SASI index expression — the one covering the fewest SSTables — by iterating all expressions and keeping track of the minimum. The comparison `if (primaryIndexes.size() > indexes.size())` is only true when a smaller set is found, but the very first expression is never selected because `primaryIndexes` starts as an empty set (size 0) and no subsequent set can be smaller. The `expression` variable therefore remained `null` after the loop, causing a `NullPointerException` or silently skipping the primary index when the query was executed.

### Before / After
```java
// BEFORE — first expression never wins because empty set (size 0) is never > anything
if (primaryIndexes.size() > indexes.size())
{
    primaryIndexes = indexes;
    expression = e;
}

// AFTER — unconditionally accept the first expression, then compare sizes
if (expression == null || primaryIndexes.size() > indexes.size())
{
    primaryIndexes = indexes;
    expression = e;
}
```

---

## How to Spot
Minimum-finding loops that start with a sentinel empty collection and compare sizes will silently keep the empty set as the "minimum" forever. The idiom `if (best == null || current < best)` is the correct initialization guard. Whenever `expression` or any accumulator can be null after a loop, either the initial condition is wrong or the loop can produce no iterations, both of which need explicit handling.

---

## Test
No test added in this commit.
