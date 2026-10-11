# Bug 0864: Double Map Lookup in LCS SSTable Grouping Loop

**Commit**: 0a5603789d · **Ticket**: n/a
**Subsystem**: Compaction / LeveledCompactionStrategy
**Category**: Logic error in condition

---

## Root Cause
In `LeveledCompactionStrategy.groupSSTablesForAntiCompaction`, the loop used `containsKey` followed by a second `get` call on the same map key, performing two lookups per iteration. The `get` call was not guarded by a null check against the returned collection, so if the map was ever accessed under any scenario where the key could be absent between the two calls (or simply in a code-pattern sense), incorrect behavior could occur. Even without a race, the idiom is semantically wrong: it always calls `get(level)` after `put` on the absent branch, meaning on the happy path (key present) it does a redundant lookup on every iteration.

### Before / After
```java
// BEFORE
if (!sstablesByLevel.containsKey(level))
{
    sstablesByLevel.put(level, new ArrayList<SSTableReader>());
}
sstablesByLevel.get(level).add(sstable);

// AFTER
Collection<SSTableReader> sstablesForLevel = sstablesByLevel.get(level);
if (sstablesForLevel == null)
{
    sstablesForLevel = new ArrayList<SSTableReader>();
    sstablesByLevel.put(level, sstablesForLevel);
}
sstablesForLevel.add(sstable);
```

---

## How to Spot
In review, look for the `containsKey` + `get` pattern on the same map with the same key inside a hot loop. The correct idiom is a single `get`, null-check, and conditional `put` — storing the result of `get` (or the newly created value) in a local variable so the third operation (the `add`) uses the local reference. Any code that calls `get` twice — once to check and once to use — has this defect.

---

## Test
No test added.
