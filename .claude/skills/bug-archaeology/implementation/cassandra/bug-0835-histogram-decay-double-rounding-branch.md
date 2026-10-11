# Bug 0835: Double Rounding in Decay-Weight Histogram Loses Precision (Branch Fix)

**Commit**: 71640f1e32 · **Ticket**: CASSANDRA-11752
**Subsystem**: Metrics / DecayingEstimatedHistogramReservoir
**Category**: Logic error in condition

---

## Root Cause
`forwardDecayWeight(now)` returned `long` by applying `Math.round()` around `Math.exp(...)`. Every exponential weight was therefore rounded to the nearest integer before being stored in the `AtomicLongArray` of decaying buckets — tiny weights (between 0 and 0.5) rounded down to zero and were never recorded, while larger weights lost all sub-integer precision. The same integer `rescaleFactor` was used during periodic rescaling, compounding the precision loss. The fix moves the rounding out of `forwardDecayWeight` (which now returns `double`) and applies `Math.round()` only at the write-to-array call-site, letting the rescale path keep full floating-point fidelity. This commit applies the same fix as ac24b88e56 to the corresponding branch.

### Before / After
```java
// BEFORE
private long forwardDecayWeight(long now) {
    return Math.round(Math.exp(((now - decayLandmark) / 1000L) / MEAN_LIFETIME_IN_S));
}
decayingBuckets.getAndAdd(index, forwardDecayWeight(now));
final long rescaleFactor = forwardDecayWeight(now);

// AFTER
private double forwardDecayWeight(long now) {
    return Math.exp(((now - decayLandmark) / 1000L) / MEAN_LIFETIME_IN_S);
}
decayingBuckets.getAndAdd(index, Math.round(forwardDecayWeight(now)));
final double rescaleFactor = forwardDecayWeight(now);
```

---

## How to Spot
When a helper method that returns a floating-point value has `Math.round()` baked into it, every caller receives a coarsened value even when full precision is needed. Rounding conversions belong at the write boundary (array store, serialization), not inside the computation itself. Also watch for branches of the same fix landing as separate commits when a bug spans multiple release lines.

---

## Test
No dedicated test added in this commit.
