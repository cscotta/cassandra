# Bug 1326: Inverted Divisor in Parallelism Re-Calculation for Secondary Index and Range Queries

**Commit**: ca6360704a · **Ticket**: CASSANDRA-8856
**Subsystem**: service/StorageProxy
**Category**: Logic error in condition

---

## Root Cause

When a range or secondary-index query fetched fewer rows than the requested limit on the first round, `StorageProxy` re-calculated `actualRowsPerRange` to decide how many additional ranges to query in parallel. The formula was written as `i / fetchedRows` (ranges scanned divided by rows returned) instead of `fetchedRows / i` (rows returned divided by ranges scanned). The inverted expression computes a "ranges per row" density rather than a "rows per range" density, producing a value orders of magnitude too small whenever `fetchedRows > i`, which made the subsequent `concurrencyFactor` absurdly large and caused the coordinator to fan out to far more ranges than necessary — or, when the value rounded to zero, stall the query.

### Before / After

```java
// BEFORE
actualRowsPerRange = i / fetchedRows;  // inverted: ranges/row instead of rows/range
concurrencyFactor = Math.max(1, Math.min(ranges.size() - i,
    Math.round(remainingRows / actualRowsPerRange)));
```

```java
// AFTER
actualRowsPerRange = fetchedRows / i;  // correct: rows/range
concurrencyFactor = Math.max(1, Math.min(ranges.size() - i,
    Math.round(remainingRows / actualRowsPerRange)));
```

---

## How to Spot

Division expressions that produce a density or rate (X per Y) should be sanity-checked by confirming the units: numerator units divided by denominator units must yield the declared variable's units. Here, `actualRowsPerRange` must have units of rows/range, so `fetchedRows [rows] / i [ranges]` is correct and `i [ranges] / fetchedRows [rows]` is wrong. A quick dimensional analysis of any ratio expression is a reliable review heuristic for this class of bug.

---

## Test

No test added.
