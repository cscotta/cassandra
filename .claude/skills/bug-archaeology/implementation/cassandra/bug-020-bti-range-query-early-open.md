# Bug 020: Fix range queries on early-open BTI files

**Commit**: 1db6f54925 · **Ticket**: CASSANDRA-20976
**Subsystem**: SSTable I/O / BTI index format
**Category**: Missing override of default method

---

## Root Cause

When a BTI (Trie-Based Index) SSTable is "early-opened" during compaction — made visible to readers before the compaction is fully complete — the partition index is represented by `PartitionIndexEarly`, a subclass of `PartitionIndex`. The base class `sharedCopy()` method creates a plain `PartitionIndex`, discarding the early-open subclass's `cutoff` and `tail` fields that are essential for reading the partially-written file. As a result, any code path that calls `sharedCopy()` (such as opening a range scanner) obtained an instance that tried to read the index as if it were complete, causing incorrect range query results. Additionally, `completedPartitionIndex()` was passing `0` as the length override to the file handle builder, which (since `0 <= 0`) was treated the same as no override, whereas the intent was to remove any previously-set length cap — the correct sentinel is `NO_LENGTH_OVERRIDE` (`-1`).

### Before / After

```java
// BEFORE — sharedCopy() in PartitionIndex loses early-open state
public PartitionIndex sharedCopy()
{
    return new PartitionIndex(this);  // loses cutoff + tail from PartitionIndexEarly
}

// completedPartitionIndex() cleared length cap with 0 (ignored by builder)
rowIndexFHBuilder.withLengthOverride(0);
partitionIndexFHBuilder.withLengthOverride(0);

// AFTER — PartitionIndexEarly overrides sharedCopy() to preserve its fields
@Override
public PartitionIndex sharedCopy()
{
    return new PartitionIndexEarly(this);  // copies cutoff + tail
}

// completedPartitionIndex() uses the correct sentinel
rowIndexFHBuilder.withLengthOverride(NO_LENGTH_OVERRIDE);
partitionIndexFHBuilder.withLengthOverride(NO_LENGTH_OVERRIDE);
```

---

## How to Spot

When a concrete subclass extends a base class that has a `sharedCopy()` / `clone()` / copy-constructor pattern, and the subclass adds new fields, a reviewer should verify that the subclass overrides every copy-producing method; failing to do so silently produces a truncated copy that is typed as the base class, losing subclass invariants. Similarly, any magic sentinel value passed to a builder method (here `0` vs. `-1`) should be expressed as a named constant — a bare `0` that is supposed to mean "no override" but is actually ignored by the "if <= 0, ignore" guard is impossible to distinguish from an intentional zero.

---

## Test

`EarlyOpenCompactionTest.testEarlyOpenDuringCompaction` writes 1 000 partitions, triggers compaction with preemptive open enabled, and concurrently runs point queries and token-range queries, asserting zero result-set errors throughout the compaction window.
