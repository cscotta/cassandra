# Bug 0622: Distribution.average() Skips Last Probability Bucket Due to Float Precision

**Commit**: 1db54a1266 · **Ticket**: CASSANDRA-14106
**Subsystem**: `tools/stress/src/org/apache/cassandra/stress/generate/Distribution.java`
**Category**: Off-by-one

---

## Root Cause

`Distribution.average()` sampled `inverseCumProb` at 50 evenly spaced probabilities by stepping a `double d` from `0.0` by `0.02` for 50 iterations, intending to cover the range `[0.0, 0.98]`. The value `1.0` (the distribution's upper bound) was never sampled, skewing the average downward. The fix changes the loop count to 51 and the accumulator type to `float` (which uses `0.02f` steps), ensuring the sequence terminates exactly at `1.0` and includes the maximum value of the distribution. Without this fix, `gaussian(-1000..-10)` returned `-504` instead of the correct `-505`, and for range `1..10` the average was computed as `5` only by coincidence of the symmetric distribution hiding the error.

### Before / After

```java
// BEFORE
double d = 0d;
for (int count = 0; count < 50; count++)
{
    sum += inverseCumProb(d);
    d += 0.02d;
}
```

```java
// AFTER
float d = 0;
for (int count = 0; count < 51; count++)
{
    sum += inverseCumProb(d);
    d += 0.02d;
}
```

---

## How to Spot

Numeric range sampling loops that use floating-point step accumulators are susceptible to precision drift, causing the last intended value to be skipped. When implementing a uniform sample over `[0, 1]`, verify that the loop count and step are chosen so that the final iteration lands exactly on `1.0`. Switching to an integer loop counter with a derived probability (`count / (N-1.0)`) avoids accumulation drift entirely.

---

## Test

`DistributionGaussianTest.negValueGaussian` asserts that `gaussian(-1000..-10).average()` returns `-504` (the correct midpoint) and that `inverseCumProb(0)` and `inverseCumProb(1)` return the min and max values.
