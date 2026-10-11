# Bug 0937: sstabledump omits partition-level deletion and static row

**Commit**: 48bdb41db5 · **Ticket**: CASSANDRA-11321
**Subsystem**: Tools / sstabledump
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SSTableExport`'s debug output loop called `partition.forEachRemaining()` to print rows but never checked `partition.partitionLevelDeletion()` or `partition.staticRow()` before entering the row loop. Partitions that carried only a partition-level tombstone or only static-column data produced no output at all in debug mode — the deletion marker and the static row were silently skipped, making the dump incomplete and misleading for diagnosis.

### Before / After
```java
// BEFORE
partitions.forEach(partition ->
{
    position.set(currentScanner.getCurrentPosition());
    partition.forEachRemaining(row ->
    {
        System.out.println(...row...);
    });
});

// AFTER
partitions.forEach(partition ->
{
    position.set(currentScanner.getCurrentPosition());
    if (!partition.partitionLevelDeletion().isLive())
    {
        System.out.println("[" + key + "]@" + position.get() + " "
                + partition.partitionLevelDeletion());
    }
    if (!partition.staticRow().isEmpty())
    {
        System.out.println("[" + key + "]@" + position.get() + " "
                + partition.staticRow().toString(metadata, true));
    }
    partition.forEachRemaining(row -> { ... });
});
```

---

## How to Spot
Whenever a tool iterates `UnfilteredPartitionIterator` or a `Partition`, review whether it also consumes `partitionLevelDeletion()` and `staticRow()` — these are not surfaced by the row iterator itself. Missing either check produces incomplete output (or skipped processing) for tables with static columns or TTL-based partition tombstones.

---

## Test
No test added.
