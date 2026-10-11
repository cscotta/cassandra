# Bug 0745: Negative mean latency metric in DecayingEstimatedHistogramReservoir

**Commit**: 7e05f393f3 · **Ticket**: CASSANDRA-12876
**Subsystem**: Metrics
**Category**: Logic error in condition

---

## Root Cause
`DecayingEstimatedHistogramReservoir` implements exponential decay ("forward decay") where each bucket's recorded count is multiplied by a time-dependent weight `forwardDecayWeight(t)`. When a snapshot is taken, the raw decaying bucket values are read directly without dividing out the current rescale factor. Because the decay weight grows over time, raw bucket values are large early and then decrease relative to older snapshots; the mean computation (sum of `value * count` divided by total count) can yield a negative result whenever the stored raw counts appear inconsistent with each other across the numerator and denominator of the mean due to not being normalized to the same time reference.

### Before / After
```java
// BEFORE — snapshot constructor copies raw decaying counts
for (int i = 0; i < length; i++)
    this.decayingBuckets[i] = reservoir.decayingBuckets.get(i);

// AFTER — normalize all counts to the same time point before storing
final double rescaleFactor = forwardDecayWeight(clock.getTime());
for (int i = 0; i < length; i++)
    this.decayingBuckets[i] = Math.round(reservoir.decayingBuckets.get(i) / rescaleFactor);
```

---

## How to Spot
Any metric reservoir that applies a time-varying weight to stored values must normalize those values to a common epoch before performing arithmetic across buckets. When a snapshot divides the sum of weighted values by the sum of weights without first cancelling the shared time factor, the ratios remain valid — but the mean is computed as `sum(value_i * rawCount_i) / sum(rawCount_i)` where `rawCount_i` values are from different atomic reads at slightly different times, making them incomparable and potentially producing a negative mean.

---

## Test
A test case was added to `DecayingEstimatedHistogramReservoirTest` verifying that the mean value is non-negative after rapid snapshot creation.
