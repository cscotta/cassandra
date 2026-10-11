# Bug 1424: Avoid IAE in IndexSummaryManager while sorting sstables

**Commit**: c93765772e · **Ticket**: CASSANDRA-8182
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexSummaryManager.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The `IndexSummaryManager` sorted SSTable readers by their fifteen-minute read rate using an inline `Comparator` that called `sstable.readMeter.fifteenMinuteRate()` directly during comparison. Because a meter's rate can change between two comparisons of the same SSTable pair, the comparator could return inconsistent results (e.g., `compare(a, b) > 0` and `compare(b, a) > 0` simultaneously), violating the contract of `Comparator` and causing `Collections.sort` to throw an `IllegalArgumentException`. The fix snapshotted each SSTable's read rate into a `HashMap` before sorting, so all comparisons used a stable set of values.

### Before / After

```java
// BEFORE
Collections.sort(sstablesByHotness, new Comparator<SSTableReader>() {
    public int compare(SSTableReader o1, SSTableReader o2) {
        ...
        return Double.compare(o1.readMeter.fifteenMinuteRate(),
                              o2.readMeter.fifteenMinuteRate()); // live, mutable rate
    }
});
```

```java
// AFTER
final Map<SSTableReader, Double> readRates = new HashMap<>(nonCompacting.size());
for (SSTableReader sstable : nonCompacting) {
    if (sstable.readMeter != null)
        readRates.put(sstable, sstable.readMeter.fifteenMinuteRate()); // snapshot
}
Collections.sort(sstablesByHotness, new ReadRateComparator(readRates)); // stable snapshot
```

---

## How to Spot

When a comparator reads a volatile or continuously-updating metric directly during each invocation, the sort key is effectively non-deterministic across calls. Reviewers should check that any `Comparator` used with `Collections.sort` (or `TreeSet`, `PriorityQueue`, etc.) derives its key from a stable, pre-computed value rather than a live mutable field. The pattern "fetch metric inside comparator body" is a red flag whenever that metric can change concurrently.

---

## Test

No test added.
