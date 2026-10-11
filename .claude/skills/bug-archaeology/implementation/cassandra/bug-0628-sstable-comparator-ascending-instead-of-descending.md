# Bug 0628: SSTable maxTimestamp Comparator Orders Ascending Instead of Descending

**Commit**: a9225f90e2 · **Ticket**: CASSANDRA-14010
**Subsystem**: `src/java/org/apache/cassandra/io/sstable/format/SSTableReader.java`
**Category**: Off-by-one

---

## Root Cause

`SSTableReader.maxTimestampComparator` was intended to sort SSTables in descending order by max timestamp so that `SinglePartitionReadCommand` could iterate the most-recently-written SSTables first and return correct results under partial reads. The comparator was implemented as `Long.compare(o1.getMaxTimestamp(), o2.getMaxTimestamp())`, which produces ascending order (smallest timestamp first). The correct implementation for descending order is to swap the arguments: `Long.compare(o2.getMaxTimestamp(), o1.getMaxTimestamp())`. As a result, reads that relied on this ordering to find the freshest data consulted SSTables in the wrong sequence, potentially returning stale cell values.

### Before / After

```java
// BEFORE – ascending order (wrong)
public static final Comparator<SSTableReader> maxTimestampComparator =
    (o1, o2) -> Long.compare(o1.getMaxTimestamp(), o2.getMaxTimestamp());
```

```java
// AFTER – descending order (correct), comment added for clarity
// Descending order
public static final Comparator<SSTableReader> maxTimestampComparator =
    (o1, o2) -> Long.compare(o2.getMaxTimestamp(), o1.getMaxTimestamp());
```

---

## How to Spot

Lambda comparators that are meant to sort in reverse order are easy to get wrong because the correct idiom (`o2` before `o1`) is visually identical to the wrong one (`o1` before `o2`). Any static comparator field that lacks an explanatory comment about its sort direction should be reviewed against all call sites to confirm the required ordering. A good habit is to add a `// Descending` or `// Ascending` comment immediately adjacent to the comparator definition, as the fix does here.

---

## Test

`SimpleQueryTest.testSStableTimestampOrdering` inserts rows across multiple flushed SSTables with explicit timestamps and asserts that a subsequent read returns the row from the highest-timestamp SSTable, not a stale one.
