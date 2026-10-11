# Bug 0744: Pending repair compaction mix wrong check and Range vs Bounds for SSTable intersects

**Commit**: 3f3db2d40d · **Ticket**: CASSANDRA-13217
**Subsystem**: Compaction / SSTable
**Category**: Logic error in condition

---

## Root Cause
Two separate logic errors introduced by the CASSANDRA-9143 incremental repair work. First, `CompactionStrategyManager.validateForCompaction()` checked whether two SSTables had different pending-repair status (`isPending != sstable.isPendingRepair()`) rather than whether they belonged to different pending-repair sessions; this prevented compacting a pending-repair SSTable with a non-pending one (correct) but also allowed two SSTables from *different* pending sessions to be compacted together (wrong). Second, `SSTableReader.intersects(Collection<Range<Token>>)` constructed a `Range` (open-closed, excludes the left endpoint) instead of a `Bounds` (closed-closed) to represent the SSTable's token span, so the SSTable was falsely reported as non-intersecting when the query range started exactly at `first`.

### Before / After
```java
// BEFORE — CompactionStrategyManager.validateForCompaction()
if (isPending != sstable.isPendingRepair())
    throw new UnsupportedOperationException("You can't compact sstables pending for repair with non-pending ones");

// AFTER
UUID pendingRepair = firstSSTable.getSSTableMetadata().pendingRepair;
...
if (isPending && !pendingRepair.equals(sstable.getSSTableMetadata().pendingRepair))
    throw new UnsupportedOperationException("You can't compact sstables from different pending repair sessions");

// BEFORE — SSTableReader.intersects()
Range<Token> range = new Range<>(first.getToken(), last.getToken());

// AFTER
Bounds<Token> range = new Bounds<>(first.getToken(), last.getToken());
```

---

## How to Spot
Whenever a boolean flag is used to compare two objects that can independently carry an identity value (here a `UUID`), the check needs to compare the identity values themselves, not just the flag. When wrapping a closed interval in a Cassandra token type, reach for `Bounds` (closed-closed) rather than `Range` (open-closed); using `Range` here was a semantic mismatch that silently dropped the first token of every SSTable from intersection tests.

---

## Test
No test added for these specific fixes (the companion CASSANDRA-9143 feature shipped extensive tests; these were minor follow-up corrections).
