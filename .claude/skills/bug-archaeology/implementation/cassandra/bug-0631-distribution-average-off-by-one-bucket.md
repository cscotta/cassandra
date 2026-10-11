# Bug 0631: Distribution.average() Samples 51 Points Over [0,1) Instead of 50

**Commit**: d8f0361228 · **Ticket**: CASSANDRA-14090
**Subsystem**: `tools/stress/src/org/apache/cassandra/stress/generate/Distribution.java`
**Category**: Off-by-one

---

## Root Cause

`Distribution.average()` estimated the mean of a probability distribution by sampling the inverse cumulative distribution function at evenly-spaced probability values. The loop used `count < 51` and a step of `0.02`, accumulating `d` starting from `0`. After 50 iterations `d` reaches `1.0`, and the 51st call `inverseCumProb(1.0)` requests the value at exactly the `1.0` quantile, which is often infinite or undefined for distributions with unbounded upper tails (e.g. normal or exponential). This caused a crash or a corrupted average. The correct approach is to sample exactly 50 points at `d` = 0.00, 0.02, …, 0.98, stopping before `d` reaches `1.0`.

### Before / After

```java
// BEFORE – 51 iterations, last call is inverseCumProb(1.0)
for (int count = 0; count < 51; count++) {
    sum += inverseCumProb(d);
    d += 0.02d;
}
```

```java
// AFTER – exactly 50 iterations, d stays in [0.0, 0.98]
for (int count = 0; count < 50; count++) {
    sum += inverseCumProb(d);
    d += 0.02d;
}
```

---

## How to Spot

Loop bounds for numerical integration or sampling should be verified against the range of the accumulator variable. When the loop variable and an increment-by-constant counter jointly control how many iterations run, verify that the final accumulated value stays strictly inside the valid input domain. Any loop that feeds a monotonically increasing `d` into a function with a restricted domain `[lo, hi)` should use `count < N` where `N * step < hi`.

---

## Test

No test added.
