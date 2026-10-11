# Bug 0259: Splitter Creates One Extra Split Due to Accumulation Rounding

**Commit**: 9213335f59 · **Ticket**: CASSANDRA-18013
**Subsystem**: src/java/org/apache/cassandra/dht/Splitter.java
**Category**: Off-by-one

---

## Root Cause

`Splitter.splitOwnedRanges` divides the token range into `parts` equal-sized slices by accumulating a running sum (`sum`) and emitting a boundary each time the sum reaches `perPart`. When the token space does not divide evenly, rounding errors in the accumulated `sum` caused the loop to emit one extra boundary before the last range was exhausted. For example, splitting a range of 32 tokens into 7 parts would produce 8 boundaries instead of 7. When Cassandra runs with assertions enabled, the assertion that checks `boundaries.size() == parts` would fail.

### Before / After

```java
// BEFORE – no guard for when boundaries.size() reaches the limit
while (currentRangeWidth.compareTo(perPart.subtract(sum)) >= 0) {
    BigInteger withinRangeBoundary = perPart.subtract(sum);
    left = left.add(withinRangeBoundary);
    boundaries.add(tokenForValue(left));
    currentRangeWidth = currentRangeWidth.subtract(withinRangeBoundary);
    sum = BigInteger.ZERO;
}
```

```java
// AFTER – track remaining tokens and break early on last boundary
tokensLeft = tokensLeft.subtract(perPart);
// ...
int partsLeft = parts - boundaries.size();
if (partsLeft == 0)
    break;
else if (partsLeft == 1)
    perPart = tokensLeft;  // absorb remaining rounding error in last slice
```

---

## How to Spot

Cumulative-division loops that compare a running sum against a fixed `perPart` quota are vulnerable to rounding drift: each iteration may accumulate a small residual, and after `n-1` boundaries the residual can push the loop into one extra iteration. Reviewers should check that such loops either (1) hard-break when the target count is reached, or (2) recompute the quota for the final interval from the actual remaining tokens rather than the original average.

---

## Test

`SplitterTest.testSplitOwnedRanges` iterates over all split counts from 1 to 31 for a range `[0, 31)` using the Murmur3Partitioner and asserts that `splitOwnedRanges(i, ...)` returns exactly `i` tokens.
