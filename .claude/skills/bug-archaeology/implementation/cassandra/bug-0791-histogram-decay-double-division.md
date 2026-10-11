# Bug 0791: DecayingEstimatedHistogramReservoir double division loses precision in decay weight

**Commit**: 801c1a5942 · **Ticket**: CASSANDRA-11752
**Subsystem**: Metrics / DecayingEstimatedHistogramReservoir
**Category**: Logic error in condition

---

## Root Cause
`forwardDecayWeight()` was declared to return `long` and internally called `Math.round(Math.exp(...))` before returning. At call sites the result was passed to `getAndAdd()` and used as a `rescaleFactor`. The double rounding meant precision was lost twice: once during the integer conversion inside `forwardDecayWeight`, and again when the already-rounded value was used in floating-point arithmetic at the call site. Because `Math.exp(...)` can return values between 0 and 1 for recent timestamps, `Math.round` could produce 0 or 1, collapsing the decay curve and causing histogram buckets to be weighted incorrectly. The fix changes the return type to `double` and moves the single `Math.round()` call to the `getAndAdd()` site, preserving full floating-point precision throughout the rescale path.

### Before / After
```java
// BEFORE — rounds inside the helper, loses precision early
private long forwardDecayWeight(long now) {
    return Math.round(Math.exp(((now - decayLandmark) / 1000L) / MEAN_LIFETIME_IN_S));
}
// call site:
decayingBuckets.getAndAdd(index, forwardDecayWeight(now));   // already-rounded long
final long rescaleFactor = forwardDecayWeight(now);          // same

// AFTER — keeps double precision; rounds only at the integer boundary
private double forwardDecayWeight(long now) {
    return Math.exp(((now - decayLandmark) / 1000L) / MEAN_LIFETIME_IN_S);
}
// call site:
decayingBuckets.getAndAdd(index, Math.round(forwardDecayWeight(now)));
final double rescaleFactor = forwardDecayWeight(now);
```

---

## How to Spot
Helper methods named with "weight", "factor", or "decay" that return integer types should be questioned: if the underlying calculation is exponential or fractional, premature rounding discards precision that is significant to downstream consumers. Review every `Math.round()` or cast-to-long that occurs inside a calculation rather than at the final integer-boundary write.

---

## Test
No test added.
