# Bug 2014: LeveledManifest Generation Count Shrinks to Zero with Large SSTable Sizes

**Commit**: 98b9cb47f9 · **Ticket**: CASSANDRA-5589
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java, src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Logic error in condition

---

## Root Cause

`LeveledManifest` allocates a `generations` array sized by `(int) Math.log10(1000 * 1000 * 1000 / maxSSTableSizeInMB)`. This formula was intended to compute how many LCS levels are needed to hold a petabyte of data at the configured SSTable size. However, when `maxSSTableSizeInMB` is large (e.g., 1000 MB or more), the division produces a value close to or less than 1, and `Math.log10()` of a value ≤ 1 returns 0 or a negative number, resulting in `n = 0`. This causes an empty or zero-length `generations` array, immediately breaking compaction. The fix removes `maxSSTableSizeInMB` from the formula entirely, always sizing for a 1-MB SSTable (the historical minimum), making the generation count independent of configuration.

### Before / After

```java
// BEFORE
int n = (int) Math.log10(1000 * 1000 * 1000 / maxSSTableSizeInMB);
```

```java
// AFTER
// allocate enough generations for a PB of data, with a 1-MB sstable size.
int n = (int) Math.log10(1000 * 1000 * 1000);
```

---

## How to Spot

When computing array sizes from user-supplied configuration values, check edge cases where the config value causes the formula to produce zero or a negative result. Formulas that divide a constant by a configurable value are especially vulnerable — as the config approaches or exceeds the constant, the result collapses. Add a warning or validation for out-of-range config values (the fix also adds a `logger.warn` for `maxSSTableSizeInMB >= 1000`).

---

## Test

No test added.
