# Bug 1821: Assertion Failure in filterColdSSTables

**Commit**: c960975950 · **Ticket**: CASSANDRA-6483
**Subsystem**: src/java/org/apache/cassandra/db/compaction
**Category**: Race condition / missing synchronization

---

## Root Cause

`SizeTieredCompactionStrategy.filterColdSSTables` and `trimToThresholdWithHotness` called `hotness(sstable)` inside a `Comparator` during `Collections.sort`. Because `hotness` reads from a live read-rate meter that can change between comparator invocations, the comparator was non-deterministic: the same two elements could compare differently on successive calls, violating the `Comparator` contract and triggering a `java.lang.IllegalArgumentException: Comparison method violates its general contract!` assertion. The fix snapshots all hotness values into an immutable map before sorting, ensuring a consistent total order. An additional early-return for `coldReadsToOmit == 0.0` avoids unnecessary work when the feature is disabled.

### Before / After

```java
// BEFORE – hotness() reads live metrics; comparator is non-deterministic
Collections.sort(sstables, new Comparator<SSTableReader>()
{
    public int compare(SSTableReader o1, SSTableReader o2)
    {
        int comparison = Double.compare(hotness(o1), hotness(o2));
        ...
    }
});
```

```java
// AFTER – snapshot taken before sort; comparator uses stable values
final Map<SSTableReader, Double> hotnessSnapshot = getHotnessMap(sstables);
Collections.sort(sstables, new Comparator<SSTableReader>()
{
    public int compare(SSTableReader o1, SSTableReader o2)
    {
        int comparison = Double.compare(hotnessSnapshot.get(o1), hotnessSnapshot.get(o2));
        ...
    }
});
```

---

## How to Spot

A `Comparator` that reads from any mutable or live-computed value is inherently unreliable in `Collections.sort`. During review, flag any comparator whose key function is not a simple field access or a value captured before the sort begins. The symptom is a non-deterministic `IllegalArgumentException` from the sort algorithm's internal invariant checks.

---

## Test

No new test added. The assertion failure was reproduced under load by CASSANDRA-6483; the fix is validated by the absence of `IllegalArgumentException` in stress testing.
