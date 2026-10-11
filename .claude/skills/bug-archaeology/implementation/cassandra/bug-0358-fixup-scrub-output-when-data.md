# Bug 0358: Scrub Completion Message Suppressed When Output SSTable Is Empty

**Commit**: e581a85b93 · **Ticket**: CASSANDRA-16835
**Subsystem**: src/java/org/apache/cassandra/db/compaction (Scrubber)
**Category**: Logic error in condition

---

## Root Cause

`Scrubber.scrub()` set a `boolean completed = false` flag inside the try block and only set it to `true` just before the `try` block exited normally.  The post-scrub summary log message was then gated on `if (completed)`.  However, when the scrubbed SSTable contained only tombstoned (empty) partitions, `writer.finish()` returned an empty list and `finished` remained empty, while `completed` was still set to `true`.  Conversely, if an `IOException` was thrown partway through, `completed` stayed `false` and no summary was printed even when partial output had been written.  The fix replaced the `completed` flag with `if (!finished.isEmpty())`, which correctly reflects whether any output SSTables were produced, and separately handles the all-tombstone case.

### Before / After

```java
// BEFORE
boolean completed = false;
...
finished.addAll(writer...finish());
completed = true;
...
if (completed)
{
    outputHandler.output("Scrub of " + sstable + " complete: " + goodRows + " rows...");
    ...
}
else
{
    ...
    outputHandler.output("Scrub of " + sstable + " complete; looks like all " + emptyRows + " rows were tombstoned");
}
```

```java
// AFTER
finished.addAll(writer...finish());
// (no completed flag)
...
if (!finished.isEmpty())
{
    outputHandler.output("Scrub of " + sstable + " complete: " + goodPartitions + " partitions...");
    ...
}
else
{
    ...
    outputHandler.output("Scrub of " + sstable + " complete; looks like all " + emptyPartitions + " partitions were tombstoned");
}
```

---

## How to Spot

Boolean "completion" flags that are set at the end of a try block are fragile: any early return or exception leaves them at their initial value.  Prefer a condition derived directly from a meaningful output artifact — such as "did we produce any output files?" — rather than a flag that must be manually set at exactly the right point.

---

## Test

`ScrubTest` updated to use renamed `goodPartitions`/`badPartitions`/`emptyPartitions` fields from `ScrubResult` and assert correct counts after scrubbing a partially corrupted SSTable.
