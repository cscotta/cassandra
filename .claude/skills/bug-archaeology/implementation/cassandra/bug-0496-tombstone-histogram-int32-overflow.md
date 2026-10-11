# Bug 0496: Tombstone Histogram Int32 Overflow

**Commit**: 00fb6d76d0 · **Ticket**: CASSANDRA-14773
**Subsystem**: `src/java/org/apache/cassandra/utils/streamhist`
**Category**: Logic error in condition

---

## Root Cause

`StreamingTombstoneHistogramBuilder` packed both the timestamp (point) and count (value) into a single `long` — high 32 bits for the point, low 32 bits for the value — and then incremented the combined `long` directly with `data[index] += delta`. When `delta` was large enough, the low-32-bit count field overflowed silently into the high-32-bit point field, corrupting the stored timestamp. Additionally, the bucket-rounding function `roundKey` used plain integer arithmetic (`point + (roundSeconds - delta)`) without overflow guards, so any deletion time near `Integer.MAX_VALUE` produced a wrapped-around negative timestamp.

The `DistanceHolder` structure also encoded distances as `(next - prev)` in the high 32 bits of a `long`, which itself overflowed when two points were far apart (large deletion times span ~2^31 seconds), causing incorrect merge ordering and silently dropping compaction tombstone data.

### Before / After

```java
// BEFORE — direct long accumulation silently overflows the count into the point field
data[index] += delta;

// Rounding that overflows near Integer.MAX_VALUE
int d = p % roundSeconds;
if (d > 0)
    return p + (roundSeconds - d);  // integer overflow possible
```

```java
// AFTER — decompose before accumulating; saturating arithmetic prevents overflow
data[index] = wrap(point, (long) unwrapValue(data[index]) + delta);

// Rounding with saturation
return saturatingCastToMaxDeletionTime((long) point + (long) bucketSize - (long) delta);

public static int saturatingCastToInt(long value) {
    return (int) (value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value);
}
```

---

## How to Spot

When a `long` field is used to pack two `int` fields together (bit-packing), any arithmetic that mutates the combined value directly — instead of extracting, updating, and re-packing the individual fields — will silently overflow one field into the other. Reviewers should confirm that every update path calls the wrap/unwrap helpers rather than doing arithmetic on the raw packed value, and that any additive expression involving an `int` point or timestamp first widens to `long` before the addition to prevent signed-integer wrap-around.

---

## Test

Tests were added in `StreamingTombstoneHistogramBuilderTest` covering large deletion-time values near `Integer.MAX_VALUE`, verifying that the histogram does not produce corrupted bucket points or negative timestamps after rounding and merging.
