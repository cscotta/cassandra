# Bug 0376: LeveledCompactionStrategy Compacts Last Level, Throws ArrayIndexOutOfBoundsException

**Commit**: 803a23b2f1 · **Ticket**: CASSANDRA-15669
**Subsystem**: src/java/org/apache/cassandra/db/compaction
**Category**: Off-by-one

---

## Root Cause

`LeveledGenerations.MAX_LEVEL_COUNT` was computed as `(int) Math.log10(1000 * 1000 * 1000)` which evaluates to 9 — but is documented as "L0 through L8", i.e. it was already the correct numeric value. However, `LeveledManifest.getNextLevel()` would attempt to push SSTables into level `MAX_LEVEL_COUNT` (index 9), which is one past the end of the array, causing an `ArrayIndexOutOfBoundsException`. The fix adds an explicit guard: when the current highest level already exceeds its target byte budget, log a warning and skip promotion instead of trying to move data to a non-existent level.

### Before / After

```java
// BEFORE – no guard against exceeding the last level
if (score > 1.001)
{
    // proceed to promote sstables to nextLevel with no bounds check
    CompactionCandidate l0Compaction = getSTCSInL0CompactionCandidate();
```

```java
// AFTER – skip promotion from the highest level with a warning
if (score > 1.001)
{
    if (i == generations.levelCount() - 1)
    {
        logger.warn("L" + i + " (maximum supported level) has " + remainingBytesForLevel + " bytes while "
                + "its maximum size is supposed to be " + maxBytesForLevel + " bytes");
        continue;
    }
    CompactionCandidate l0Compaction = getSTCSInL0CompactionCandidate();
```

---

## How to Spot

When a manifest scans levels to find which one is over-budget, always verify there is a guard preventing the scan from attempting to schedule compaction work that would move data to `level + 1` when `level` is already at the maximum index. A missing upper-bound check on an array-indexed loop over level structures is a classic fence-post hazard.

---

## Test

`LeveledCompactionStrategyTest` was extended with a scenario that forces a table large enough to overflow the last level, verifying no `ArrayIndexOutOfBoundsException` is thrown.
