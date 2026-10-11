# Bug 1984: Assertion Error During Repair from Overly Restrictive DataRange Assertion

**Commit**: 4f0d7b456a · **Ticket**: CASSANDRA-5757
**Subsystem**: src/java/org/apache/cassandra/db/DataRange.java, src/java/org/apache/cassandra/io/sstable/SSTableScanner.java, src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Logic error in condition

---

## Root Cause

`DataRange`'s constructor contained an assertion that rejected wrap-around ranges, but wrap-around ranges are legitimate when the `DataRange` is used for full-table scans (e.g., during repair). The assertion was valid only for paging ranges, where wrap-around semantics are undefined. Moving the assertion to the `Paging` subclass and implementing proper wrap-around support in `SSTableScanner` fixes the failure without weakening the constraint where it matters.

Additionally, `SSTableScanner` did not handle wrap-around key ranges: it would seek to the start key before reading, which caused it to miss data at the beginning of the file when the range wrapped around. The fix adds a two-phase read: first read from the file beginning up to `stopAt`, then seek to `startKey` and continue to end-of-file.

### Before / After

```java
// BEFORE: assertion in DataRange constructor — fires for repair's wrap-around range
public DataRange(AbstractBounds<RowPosition> range, IDiskAtomFilter columnFilter)
{
    assert !(range instanceof Range) || !((Range)range).isWrapAround() || range.right.isMinimum() : range;
    ...
}
```

```java
// AFTER: assertion moved to PagingAwareFilter subclass only
public DataRange(AbstractBounds<RowPosition> range, IDiskAtomFilter columnFilter)
{
    // no assertion here
    ...
}

// In PagingAwareFilter constructor:
assert !(range instanceof Range) || !((Range)range).isWrapAround() || range.right.isMinimum() : range;
```

---

## How to Spot

Watch for assertions placed in base class constructors that enforce a constraint only valid for a subset of callers. If a subclass or specific usage pattern (paging vs. full scan) tightens a contract, the assertion belongs in the more restricted context, not the general one.

---

## Test

No new test added for the assertion relocation; the repair path exercise is covered by the existing repair integration tests.
