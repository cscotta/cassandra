# Bug 0539: LegacyLayout RangeTombstone IOOBE with Asymmetric Clustering Bounds

**Commit**: 2b10a5f2b5 · **Ticket**: CASSANDRA-15172
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`
**Category**: Off-by-one

---

## Root Cause

When serializing a range tombstone into legacy (pre-3.0) format, the code iterated using `start.bound.clustering().size()` as the loop bound for both the start and end builders. Because `clustering()` returns the common clustering prefix size rather than the bound's own component count, a range tombstone with asymmetric bounds—where the start and end have a different number of clustering components—would either under-populate the end builder or cause an `IndexOutOfBoundsException` when accessing an index beyond the end bound's actual size. The fix uses each bound's own `size()` method and splits the single loop into two independent loops.

### Before / After

```java
// BEFORE – uses start.bound.clustering().size() for both builders
for (int j = 0; j < start.bound.clustering().size(); j++)
{
    startBuilder.add(start.bound.get(j));
    endBuilder.add(end.bound.get(j));  // IndexOutOfBoundsException if end is shorter
}
```

```java
// AFTER – each bound uses its own size
for (int j = 0; j < start.bound.size(); j++)
    startBuilder.add(start.bound.get(j));
for (int j = 0; j < end.bound.size(); j++)
    endBuilder.add(end.bound.get(j));
```

---

## How to Spot

When a loop drives iteration over two parallel data structures using the size of only one of them, ask whether both structures are guaranteed to have the same length. In the legacy serialization path, range tombstone bounds are not required to be symmetric in clustering depth, so any shared-counter loop is suspect. Look for patterns where `a.get(j)` and `b.get(j)` appear inside the same loop but the bound is derived from `a` alone.

---

## Test

Added `testAsymmetricRTBoundSerializedSize` which creates a range tombstone with a two-component start bound and a one-component end bound, then calls both `fromUnfilteredRowIterator` and `serializedSizeAsLegacyPartition` to verify neither throws.
