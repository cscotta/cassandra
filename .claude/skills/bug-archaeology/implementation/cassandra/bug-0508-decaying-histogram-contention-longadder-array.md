# Bug 0508: DecayingEstimatedHistogramReservoir Contention from LongAdder Array Layout

**Commit**: adc3cdde2a · **Ticket**: CASSANDRA-15213
**Subsystem**: `src/java/org/apache/cassandra/metrics/DecayingEstimatedHistogramReservoir.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

The histogram used two `LongAdder[]` arrays (one for decaying weights, one for raw counts) where each logical bucket was a separate heap-allocated object. Logically adjacent buckets allocated at nearly the same time share cache lines, causing heavy false-sharing contention under concurrent updates. Additionally, `Arrays.binarySearch` was used to locate the target bucket on every `update()` call, which is O(log n) and cache-unfriendly. The fix replaces the object arrays with `AtomicLongArray` (a single contiguous primitive array), adds per-thread bucket striping (each logical bucket is stored in `nStripes` physical slots selected by thread ID), and uses a fast O(1) logarithm-based index lookup (`fastLog12`) to avoid binary search on the hot path.

### Before / After

```java
// BEFORE
private final LongAdder[] decayingBuckets; // heap objects, false sharing
private final LongAdder[] buckets;

public void update(long value) {
    int index = Arrays.binarySearch(bucketOffsets, value); // O(log n)
    if (index < 0) index = -index - 1;
    decayingBuckets[index].add(Math.round(forwardDecayWeight(now)));
    buckets[index].increment();
}
```

```java
// AFTER
private final AtomicLongArray decayingBuckets; // contiguous primitive array
private final AtomicLongArray buckets;

public void update(long value) {
    int index = findIndex(bucketOffsets, value); // O(1) fast log estimate
    updateBucket(decayingBuckets, index, Math.round(forwardDecayWeight(now)));
    updateBucket(buckets, index, 1);
}

public void updateBucket(AtomicLongArray buckets, int index, long value) {
    int stripe = (int) (Thread.currentThread().getId() & (nStripes - 1));
    buckets.addAndGet(stripedIndex(index, stripe), value);
}
```

---

## How to Spot

A metrics reservoir implemented as an array of heap-allocated counter objects (e.g., `LongAdder[]`) will suffer false sharing whenever many threads write to adjacent buckets simultaneously. The tell-tale sign is the combination of a heap-object array and a tight update loop without any stripe or padding. Reviewers should also check whether hot-path index lookup uses a binary search that could be replaced with a closed-form mathematical estimate.

---

## Test

`DecayingEstimatedHistogramReservoirTest#testStriping` launches `nStripes * 2` threads each recording 1,000,000 random values and asserts that the striped reservoir produces identical mean, min, max, median, and percentile values as a single-stripe reference reservoir; it also verifies that physical striped slots beyond the logical bucket count contain non-zero values, confirming striping actually occurred.
