# Bug 2221: Infinite Recompaction Loop in LCS L0 When Only One SSTable Exists

**Commit**: f34bd79b9a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java
**Category**: Logic error in condition

---

## Root Cause

The L0 candidate-selection logic in `LeveledManifest` collected all L0 SSTables into a candidate set and then expanded it with all overlapping L1 SSTables. When there was only a single L0 SSTable with no L1 overlap, `candidates` contained exactly one element. The method returned this single-element set, causing the compaction manager to schedule a "compaction" of one file against itself, which produced a new SSTable of the same content, triggering the same condition again in an infinite loop. The fix returns an empty list when fewer than two candidates are found, so the compaction manager recognises there is nothing to do.

### Before / After

```java
// BEFORE
return candidates;
```

```java
// AFTER
return candidates.size() > 1 ? candidates : Collections.<SSTableReader>emptyList();
```

---

## How to Spot

When a compaction candidate selection routine returns whatever set it builds without a minimum-size guard, check whether a single-element result is meaningful. Compaction of a single file into itself is a no-op that produces a replacement file, restarting the selection cycle indefinitely.

---

## Test

No test added.
