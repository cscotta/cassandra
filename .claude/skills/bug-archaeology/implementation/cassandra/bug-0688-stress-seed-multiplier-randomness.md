# Bug 0688: Stress Tool Seed Values Had Insufficient Randomness

**Commit**: f1f5f19462 · **Ticket**: CASSANDRA-12744
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/generate/SeedManager.java
**Category**: Wrong constant / default value

---

## Root Cause

The stress tool generated seeds by using the raw distribution value (e.g., 1..N for a small N) as the seed directly. Guava's Murmur3-based random generator only spreads values widely when given a large input space; when the seed range is small (e.g., 100 elements), the generated row keys clustered near each other in hash-space, so many "different" operations actually exercised the same physical data. The fix introduces a `sampleMultiplier` — scaled so that the product covers roughly 22 decimal digits — and multiplies every raw seed by it before it is used, giving the entire `long` range effective coverage.

### Before / After

```java
// BEFORE — seed range stays in 1..N
public Seed next(int visits) {
    return new Seed(distribution.next(), visits);
}
// SeriesGenerator
return new Seed(start + (next % totalCount), visits);
```

```java
// AFTER — seeds spread across long space
this.sampleMultiplier = 1 + Math.round(Math.pow(10D, 22 - Math.log10(sampleSize)));
// ...
public Seed next(int visits) {
    return new Seed(distribution.next() * multiplier, visits);
}
// SeriesGenerator
return new Seed((start + (next % totalCount)) * multiplier, visits);
// LookbackableWriteGenerator guard also adjusted:
if (seed.seed / multiplier <= writeCount.get()) return;
```

---

## How to Spot

When a tool wraps a distribution value (or counter) as the primary key or seed, review whether the raw value spans enough of the target key-space. If the distribution has a narrow range (e.g., `revisit` configured to revisit only 100 rows), the bare index will cluster data near hash(0)..hash(N), missing the rest of the ring and producing non-representative load. A red flag is any seed-generation path that passes `distribution.next()` directly as a seed without any amplification or mixing step.

---

## Test

No dedicated unit test added; the fix is verified implicitly by observing that the stress tool now exercises the full token ring when a small revisit window is configured.
