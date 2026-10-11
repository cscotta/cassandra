# Bug 2245: Incorrect Midpoint Calculation for Wrapping Token Ranges in Murmur3Partitioner

**Commit**: 0ea28c858e · **Ticket**: CASSANDRA-4621
**Subsystem**: src/java/org/apache/cassandra/dht/Murmur3Partitioner.java
**Category**: Off-by-one

---

## Root Cause

`Murmur3Partitioner.midPoint()` handles wrapping ranges (where the right endpoint is numerically less than the left) separately from non-wrapping ones. The original formula `(max + r - l) >> 1 + l` was algebraically incorrect: it did not account for the true length of the wrapping arc, which spans from `l` up to `max` and then from `min` to `r`. The correct total length is `(max - l) + (r - min)`, so the midpoint starting from `l` is `(l + r + max - min) / 2`, with a wrap-around correction if the result exceeds `max`.

### Before / After

```java
// BEFORE
BigInteger max = BigInteger.valueOf(MAXIMUM);
midpoint = max.add(r).subtract(l).shiftRight(1).add(l).mod(max);
```

```java
// AFTER
BigInteger max = BigInteger.valueOf(MAXIMUM);
BigInteger min = BigInteger.valueOf(MINIMUM.token);
// (L + R + max - min) / 2
midpoint = (max.subtract(min).add(l).add(r)).shiftRight(1);
if (midpoint.compareTo(max) > 0)
    midpoint = min.add(midpoint.subtract(max));
```

---

## How to Spot

Midpoint computations on ring/modular arithmetic spaces must account for the minimum value (`MINIMUM`) as the true lower bound, not just `0`. Omitting `min` from wrapping range calculations introduces a systematic bias that shifts all midpoints by `min/2`.

---

## Test

No test added.
