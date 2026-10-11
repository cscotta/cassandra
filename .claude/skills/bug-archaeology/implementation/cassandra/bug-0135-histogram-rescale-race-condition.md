# Bug 0135: Race Condition in DecayingEstimatedHistogramReservoir During Rescale

**Commit**: ad4ad5ef5a · **Ticket**: CASSANDRA-19365
**Subsystem**: src/java/org/apache/cassandra/metrics
**Category**: Race condition / missing synchronization

---

## Root Cause

The histogram's forward-decay rescale procedure used an `AtomicBoolean rescaling` flag to allow only one thread to rescale while all others continued updating the shared `AtomicLongArray decayingBuckets` without blocking. This meant that threads that lost the CAS on `rescaling` kept writing new decay-weighted values into the old `decayingBuckets` array using the old `decayLandmark`, while the winner was simultaneously dividing the same array by the new rescale factor and then updating `decayLandmark`. The net result was that values written by losers—computed against the old landmark—were then rescaled as if they were against the new landmark, corrupting the histogram's running totals. The fix replaces the mutable `AtomicLongArray` + `volatile long` pair with an immutable `DecayingBuckets` object (landmark + array) that is atomically swapped via `AtomicReferenceFieldUpdater`; any thread that races with a rescale simply retries with the newly installed object.

### Before / After

```java
// BEFORE — shared mutable array; rescaling flag does not protect concurrent updates
private final AtomicLongArray decayingBuckets;
private volatile long decayLandmark;
private final AtomicBoolean rescaling = new AtomicBoolean(false);

private void rescaleIfNeeded(long now) {
    if (needRescale(now) && rescaling.compareAndSet(false, true)) {
        try { rescale(now); }
        finally { decayLandmark = now; rescaling.set(false); }
    }
}
```

```java
// AFTER — immutable snapshot swapped atomically; CAS retry on contention
private volatile DecayingBuckets decayingBuckets; // landmark + array, immutable pair

private DecayingBuckets rescaleIfNeeded(long now) {
    DecayingBuckets buckets = decayingBuckets;
    while (now - buckets.decayLandmark > landmarkResetIntervalInNs) {
        DecayingBuckets newBuckets = new DecayingBuckets(now);
        // copy scaled values...
        if (decayingBucketsUpdater.compareAndSet(this, buckets, newBuckets))
            return newBuckets;
        buckets = decayingBuckets; // retry with winner's new object
    }
    return buckets;
}
```

---

## How to Spot

The pattern of using a boolean CAS to "elect one winner for a write operation while others continue writing to the same data structure" is inherently unsafe: the losers continue with stale state that the winner is simultaneously modifying. Whenever a rescale or decay landmark update must be atomic with respect to ongoing updates, either all writers must observe the new landmark atomically, or the data structure being modified must be replaced atomically (copy-on-write style) so in-flight writers using the old reference are harmless.

---

## Test

`DecayingEstimatedHistogramReservoirTest` adds concurrent update and rescale scenarios; the fix exposes a configurable `landmarkResetIntervalInNs` to force frequent rescales during the test while concurrent threads call `update`, then asserts that no bucket values go negative or overflow.
