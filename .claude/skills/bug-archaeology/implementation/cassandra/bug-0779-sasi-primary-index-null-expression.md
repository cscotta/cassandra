# Bug 0779: SASI primary index not selected when expression is null on first iteration

**Commit**: 7b1e2ed6e3 · **Ticket**: CASSANDRA-12910
**Subsystem**: Secondary Indexes / SASI
**Category**: Logic error in condition

---

## Root Cause
`QueryController.getPrimaryIndexIterator()` selects the SASI index with the fewest matching SSTables as the "primary" index to drive iteration, falling back to others for post-filtering. The selection loop compared `primaryIndexes.size() > indexes.size()` to decide whether to replace the current best candidate — but on the very first iteration `expression` was still `null` and `primaryIndexes` was an empty set (size 0). An empty set is never larger than any non-empty set, so the condition was always `false` on the first pass and no primary index was ever assigned, leaving `expression == null` throughout and causing subsequent logic to fail or produce wrong results.

### Before / After
```java
// BEFORE
if (primaryIndexes.size() > indexes.size())
{
    primaryIndexes = indexes;
    expression = e;
}

// AFTER
if (expression == null || primaryIndexes.size() > indexes.size())
{
    primaryIndexes = indexes;
    expression = e;
}
```

---

## How to Spot
In any "find minimum" loop that initialises both the best-value holder and its associated data to a sentinel (null/empty), the replacement condition must special-case the first iteration. The classic pattern is `if (best == null || current < best)`. Whenever you see a raw size/count comparison without a null guard on the first iteration, the minimum-selection logic will silently skip all candidates if the initial sentinel already satisfies the comparison direction.

---

## Test
No test added.
