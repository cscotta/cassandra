# Bug 0834: Double Rounding in Decay-Weight Histogram Loses Precision

**Commit**: ac24b88e56 · **Ticket**: CASSANDRA-11752
**Subsystem**: Metrics / DecayingEstimatedHistogramReservoir
**Category**: Logic error in condition

---

## Root Cause
`forwardDecayWeight(now)` returned `long` by applying `Math.round()` around `Math.exp(...)`. Every exponential weight was therefore rounded to the nearest integer before being stored in the `AtomicLongArray` of decaying buckets — tiny weights (between 0 and 0.5) rounded down to zero and were never recorded, while larger weights lost all sub-integer precision. The same integer `rescaleFactor` was used during periodic rescaling (`rescale()`), compounding the precision loss across the lifetime of the histogram. The fix moves the rounding out of `forwardDecayWeight` (which now returns `double`) and applies `Math.round()` only at the one call-site that writes to the long-backed atomic array, letting the rescale path keep full floating-point fidelity.

### Before / After
```java
// BEFORE
private long forwardDecayWeight(long now) {
    return Math.round(Math.exp(((now - decayLandmark) / 1000L) / MEAN_LIFETIME_IN_S));
}
// call-sites:
decayingBuckets.getAndAdd(index, forwardDecayWeight(now));
final long rescaleFactor = forwardDecayWeight(now);

// AFTER
private double forwardDecayWeight(long now) {
    return Math.exp(((now - decayLandmark) / 1000L) / MEAN_LIFETIME_IN_S);
}
// call-sites:
decayingBuckets.getAndAdd(index, Math.round(forwardDecayWeight(now)));
final double rescaleFactor = forwardDecayWeight(now);
```

---

## How to Spot
When a helper method that returns a floating-point value has `Math.round()` baked into it, every caller receives a coarsened value even when full precision is needed. Review helper methods that return numeric types: rounding conversions belong at the write boundary (array store, serialization), not inside the computation itself.

---

## Test
No dedicated test added in this commit; correctness is covered implicitly by histogram precision regression tests associated with CASSANDRA-11752.
