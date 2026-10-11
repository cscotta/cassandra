# Bug 1950: LCS L0 compaction candidate check placed inside wrong branch, allowing L1 overlaps

**Commit**: ad8f88e212 · **Ticket**: CASSANDRA-5907
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java
**Category**: Logic error in condition

---

## Root Cause

In Leveled Compaction Strategy (LCS), when building an L0 compaction candidate set the code first unioned in all overlapping L1 SSTables, then — inside the same `if` block — checked whether any of the candidates or their L0 overlaps were already being compacted, returning early if so. Because the "already compacting" guard was nested inside the `if (candidates.size() > 0)` block and the final size check `candidates.size() > 1` was also inside that same block, the overlap-with-compacting-L0 check could be skipped when the block was not entered. This allowed compaction jobs that overlapped with in-progress L0 compactions to proceed, which could produce overlapping SSTables in L1 and violate LCS invariants. The fix moves both the compacting-L0 guard and the minimum-size check outside the branch so they apply unconditionally to all candidate sets.

### Before / After

```java
// BEFORE — guard and size check inside if-block; can be skipped
candidates = Sets.union(candidates, overlapping(candidates, generations[1]));
// check overlap with L0 compacting sstables to make sure we are not generating overlap in L1.
Iterable<SSTableReader> compactingL0 = Iterables.filter(generations[0], Predicates.in(compacting));
if (!Sets.intersection(candidates, compacting).isEmpty() || !overlapping(candidates, compactingL0).isEmpty())
    return Collections.emptyList();
}  // end of if-block

return candidates.size() > 1 ? candidates : Collections.<SSTableReader>emptyList();
```

```java
// AFTER — guard and size check always evaluated
candidates = Sets.union(candidates, overlapping(candidates, generations[1]));
}  // end of if-block

// check overlap with L0 compacting sstables to make sure we are not generating overlap in L1.
Iterable<SSTableReader> compactingL0 = Iterables.filter(generations[0], Predicates.in(compacting));
if (candidates.size() < 2 || !Sets.intersection(candidates, compacting).isEmpty() || !overlapping(candidates, compactingL0).isEmpty())
    return Collections.emptyList();
else
    return candidates;
```

---

## How to Spot

Safety guards placed inside conditional branches that are not always taken can silently become no-ops for certain code paths. In compaction candidate selection, any guard that ensures isolation invariants (no overlaps, no interference with in-progress jobs) must sit outside all optional branches so it is always evaluated before returning a candidate set.

---

## Test

No test added.
