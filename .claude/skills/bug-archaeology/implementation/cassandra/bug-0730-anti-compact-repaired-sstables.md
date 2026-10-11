# Bug 0730: Anti-compaction applied to already-repaired SSTables

**Commit**: 06316df549 · **Ticket**: CASSANDRA-13153
**Subsystem**: Compaction / Repair
**Category**: Logic error in condition

---

## Root Cause
`CompactionManager.performAnticompaction()` passed the full set of SSTables — both repaired and unrepaired — to `groupSSTablesForAntiCompaction()`. Anti-compaction is the post-repair step that rewrites SSTables to mark the ranges that were verified as repaired. Running it on an SSTable that is already fully repaired is at best wasteful (it produces unnecessary write amplification) and at worst causes correctness inconsistencies, because splitting an already-repaired SSTable can result in fragments with different `repairedAt` timestamps. The fix filters out already-repaired SSTables before grouping, so only unrepaired data is subject to anti-compaction.

### Before / After
```java
// BEFORE
Collection<Collection<SSTableReader>> groupedSSTables =
    cfs.getCompactionStrategy().groupSSTablesForAntiCompaction(repaired.originals());

// AFTER
Set<SSTableReader> unrepairedSSTables = ImmutableSet.copyOf(
    Iterables.filter(sstables, input -> !input.isRepaired()));
Collection<Collection<SSTableReader>> groupedSSTables =
    cfs.getCompactionStrategy().groupSSTablesForAntiCompaction(unrepairedSSTables);
```

---

## How to Spot
Any code that dispatches a compaction-like operation over a set of SSTables without first filtering on repair status should be reviewed. The guard `isRepaired()` exists precisely to distinguish data that has already been through a full repair cycle; passing repaired SSTables into an operation that modifies `repairedAt` metadata can corrupt the repaired/unrepaired classification and confuse future incremental repair decisions.

---

## Test
No new test added in this commit.
