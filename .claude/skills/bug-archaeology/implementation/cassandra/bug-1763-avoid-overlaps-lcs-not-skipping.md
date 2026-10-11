# Bug 1763: LCS Level Promotion Creates Overlapping SSTables

**Commit**: 15ee94897d · **Ticket**: CASSANDRA-6688
**Subsystem**: db/compaction/LeveledManifest
**Category**: Logic error in condition

---

## Root Cause

Two related bugs in `LeveledManifest` allowed LCS to produce overlapping SSTables, violating the core LCS invariant. First, the `skipLevels()` method would promote a compaction's output above its natural level when the output size exceeded the target level's budget and the next level was empty — but this skipping could land SSTables in a level where they overlapped with concurrently compacting SSTables. Second, when expanding the L0 candidate set by adding SSTables that overlapped with compacting L0 files, the code unconditionally added every `overlappedL0` entry to the candidate set, including SSTables that only overlapped other already-overlapping candidates but did not themselves overlap any currently compacting SSTable; these extra SSTables could introduce key range overlaps.

### Before / After

```java
// BEFORE — skipLevels() called during level assignment
newLevel = minimumLevel == maximumLevel ? maximumLevel + 1 : maximumLevel;
newLevel = skipLevels(newLevel, sstables); // could place output in wrong level
```

```java
// AFTER — skipLevels() removed entirely
newLevel = minimumLevel == maximumLevel ? maximumLevel + 1 : maximumLevel;
// no skipping; level is determined purely by the compacted range
```

```java
// BEFORE — unconditional addition of overlapped L0 candidates
for (SSTableReader newCandidate : overlappedL0)
{
    candidates.add(newCandidate);
    remaining.remove(newCandidate);
}
```

```java
// AFTER — only add if candidate doesn't overlap any compacting SSTable
for (SSTableReader newCandidate : overlappedL0)
{
    if (overlapping(newCandidate, compactingL0).isEmpty())
        candidates.add(newCandidate);
    remaining.remove(newCandidate);
}
```

---

## How to Spot

In any tiered/leveled structure where an invariant (no overlapping key ranges per level) must hold after every mutation, the candidate expansion loop must guard against transitively pulled-in members that would violate the invariant. Also beware of "optimization" methods that skip levels — they should be proven safe against concurrent compactions, not just against the current level's byte budget.

---

## Test

No test added.
