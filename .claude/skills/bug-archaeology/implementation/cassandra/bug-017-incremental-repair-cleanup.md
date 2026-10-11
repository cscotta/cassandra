# Bug 017: Fix cleanup of old incremental repair sessions in case of owned token range changes or a table deleting

**Commit**: 8d2c11ed2e · **Ticket**: CASSANDRA-20877
**Subsystem**: Repair / Consistent (Incremental) Repair
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`LocalSessions.isSuperseded()` determined whether a previous incremental repair session could be cleaned up by checking whether every repaired table had a `minRepairedAt` timestamp greater than the session's own `repairedAt` value. Two problems existed: first, if a table had since been dropped, `repairedStates.get(tid)` returned `null` and the method immediately returned `false` (treating the missing table as not-superseded), so the session was never cleaned up. Second, the method evaluated `minRepairedAt` against the session's original token ranges without considering that some of those ranges may have been transferred to a newly-added node; this caused sessions covering ranges no longer owned by the local node to appear as not-superseded, again preventing cleanup.

### Before / After

```java
// BEFORE — no null check for deleted table; uses full session ranges ignoring ownership changes
for (TableId tid : session.tableIds)
{
    RepairedState state = repairedStates.get(tid);
    if (state == null)
        return false;
    long minRepaired = state.minRepairedAt(session.ranges);
    if (minRepaired <= session.repairedAt)
        return false;
}

// AFTER — skip deleted tables; intersect session ranges with currently owned ranges
for (TableId tid : session.tableIds)
{
    TableMetadata tableMetadata = getTableMetadata(tid);
    if (tableMetadata == null) // if a table was removed - ignore it
        continue;
    RepairedState state = repairedStates.get(tid);
    if (state == null)
        return false;
    Collection<Range<Token>> actualRanges = rangesPerKeyspaceCache.computeIfAbsent(
        tableMetadata.keyspace, (keyspace) -> {
            List<Range<Token>> localRanges = getLocalRanges(tableMetadata.keyspace);
            if (localRanges.isEmpty())
                return session.ranges;
            return Range.intersect(session.ranges, localRanges);
        });
    long minRepaired = state.minRepairedAt(actualRanges);
    if (minRepaired <= session.repairedAt)
        return false;
}
```

---

## How to Spot

When iterating a set of identifiers (table IDs, node IDs, range descriptors) that were recorded at some earlier point in time, always consider that the referenced entities may no longer exist or may have partially changed ownership. A missing-entity check that immediately short-circuits with a conservative result (`return false` / "not safe to clean up") rather than skipping can silently accumulate stale state forever. Similarly, range-based checks should intersect stored ranges with currently-owned ranges before evaluating repaired-ness, not assume the local node still owns everything it did at repair time.

---

## Test

`IncrementalRepairCleanupAfterNodeAddingTest` starts a 3-node cluster, runs incremental repair, adds a fourth node (causing token range rebalancing), runs repair again, and then asserts that `cleanup()` leaves exactly one (the latest) FINALIZED session rather than two.
