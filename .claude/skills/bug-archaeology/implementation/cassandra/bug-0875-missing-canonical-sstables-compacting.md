# Bug 0875: Missing SSTables When Getting Canonical SSTable Set

**Commit**: bc23632f20 · **Ticket**: CASSANDRA-11996
**Subsystem**: Storage / SSTable lifecycle (View)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`View.select(SSTableSet.CANONICAL)` previously filtered the live `sstables` set, relying on an EARLY/MOVED_START check to map from the partially-opened replacement back to its canonical predecessor. When a compaction was in flight, the original SSTable moved into `compacting` and was no longer in `sstables`, so the filter never saw it and it was omitted from the canonical set entirely. The fix builds the canonical set by iterating both `compacting` and `sstables` separately: compacting SSTables that are not EARLY are canonical in their own right, and live SSTables that are not in `compacting` and not EARLY are also canonical. This ensures no SSTable is dropped simply because it happens to be under compaction.

### Before / After
```java
// BEFORE — only filtered the live sstables set; compacting entries invisible
case CANONICAL:
    return transform(filter(sstables,
                            (s) -> s.openReason != SSTableReader.OpenReason.EARLY),
                     (s) -> s.openReason != SSTableReader.OpenReason.MOVED_START
                            ? s : compactingMap.get(s));

// AFTER — unions compacting + sstables, both checked independently
case CANONICAL:
    Set<SSTableReader> canonicalSSTables = new HashSet<>();
    for (SSTableReader sstable : compacting)
        if (sstable.openReason != SSTableReader.OpenReason.EARLY)
            canonicalSSTables.add(sstable);
    for (SSTableReader sstable : sstables)
        if (!compacting.contains(sstable) && sstable.openReason != SSTableReader.OpenReason.EARLY)
            canonicalSSTables.add(sstable);
    return canonicalSSTables;
```

---

## How to Spot
Whenever a method computes "the full set of X" by filtering only one internal data structure, check whether another sibling structure (here, `compacting`) can hold members that belong to the same logical set. In SSTable lifecycle code, `sstables` and `compacting` together form the universe; iterating only one will produce silent omissions.

---

## Test
`SSTableRewriterTest` gained coverage for this case, verifying that canonical sstables are reported correctly while compaction is in progress.
