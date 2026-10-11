# Bug 1559: Fix Range Merging When DES Scores Are Zero

**Commit**: a4544615df · **Ticket**: CASSANDRA-7535
**Subsystem**: src/java/org/apache/cassandra/locator/DynamicEndpointSnitch.java
**Category**: Logic error in condition

---

## Root Cause

`DynamicEndpointSnitch.shouldMerge()` decided whether two token ranges could be merged into a single read by comparing the max latency score of the merged endpoint set against the sum of the individual max scores: `maxMerged < maxL1 + maxL2`. When all scores were zero (e.g., a freshly started node with no history), this evaluated to `0 < 0`, which is false — so ranges were never merged even though zero-score endpoints are effectively equivalent. The fix changes the comparison to allow merging when the merged score is no worse than a configurable preference factor (1.5×) of the combined individual scores, and uses `<=` so that the zero-zero case correctly allows merging.

### Before / After

```java
// BEFORE
return maxMerged < maxL1 + maxL2;
```

```java
// AFTER
private double RANGE_MERGING_PREFERENCE = 1.5;
// ...
return maxMerged <= (maxL1 + maxL2) * RANGE_MERGING_PREFERENCE;
```

---

## How to Spot

Comparisons used to decide "good enough to merge/combine" should use `<=` rather than `<` when the baseline case involves equal values (including zero). A strict `<` will reject the merge when both sides are identical, which is the opposite of the intended behavior.

---

## Test

No test added.
