# Bug 1409: Avoid Overlap in L1 When L0 Contains Many Non-Overlapping SSTables

**Commit**: 4868854934 · **Ticket**: CASSANDRA-8211
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When building the candidate set for an L0→L1 compaction, the code used `overlapping(newCandidate, compactingL0)` to decide whether an L0 SSTable needed to be included. This check tests whether `newCandidate` overlaps any *individual* SSTable already in the compacting set, but when the compacting SSTables are themselves non-overlapping their individual key ranges may not cover `newCandidate` even though the union of those ranges does. As a result, some L0 candidates were incorrectly skipped, causing their key ranges to end up written directly into L1 and creating overlaps in that level, violating the LCS invariant.

### Before / After

```java
// BEFORE
if (overlapping(newCandidate, compactingL0).isEmpty())
    candidates.add(newCandidate);
```

```java
// AFTER
// Compute the full key span of all compacting L0 sstables first
RowPosition lastCompactingKey = null;
RowPosition firstCompactingKey = null;
for (SSTableReader candidate : compactingL0)
{
    if (firstCompactingKey == null || candidate.first.compareTo(firstCompactingKey) < 0)
        firstCompactingKey = candidate.first;
    if (lastCompactingKey == null || candidate.last.compareTo(lastCompactingKey) > 0)
        lastCompactingKey = candidate.last;
}
// ...
if (firstCompactingKey == null || lastCompactingKey == null ||
    overlapping(firstCompactingKey.getToken(), lastCompactingKey.getToken(),
                Arrays.asList(newCandidate)).size() == 0)
    candidates.add(newCandidate);
```

---

## How to Spot

When a set of objects is tested for overlap against a *collection*, check whether the logic uses point-wise membership (does X overlap any individual element?) versus span-wise membership (does X overlap the union of all elements?). In LCS compaction, L0 SSTables are allowed to overlap each other, so the compacting set can have gaps between individual members while still collectively covering a key range. Any code that checks `overlapping(x, set).isEmpty()` where the set members may themselves be non-contiguous is a candidate for this bug.

---

## Test

No test added.
