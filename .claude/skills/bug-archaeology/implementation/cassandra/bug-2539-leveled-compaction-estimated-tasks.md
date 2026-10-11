# Bug 2539: Leveled compaction estimated task count uses wrong units and allows negative value

**Commit**: 173d0d8469 · **Ticket**: n/a

## Detection Rule
When computing a compaction task count as `(totalBytes - maxBytes) / unitBytes`, ensure the unit divisor includes the full byte conversion factor and wrap the subtraction in `Math.max(0, ...)` to prevent a negative count when a level is within bounds.

## Code Smell
`n += (getTotalBytes(sstables) - maxBytesForLevel(i)) / maxSSTableSizeInMB` — `maxSSTableSizeInMB` is in megabytes but `getTotalBytes()` returns bytes, so the quotient is 1,048,576× too large; also the subtraction is not guarded and can be negative when the level is under capacity.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: missing-unit-conversion-in-divisor
- severity: performance
- subsystem: compaction

## Root Cause
`LeveledManifest.getEstimatedTasks()` summed up how many compactions are needed across all levels. For each level it computed the excess bytes divided by the max SSTable size. The max SSTable size variable `maxSSTableSizeInMB` was in MiB but was used as if it were bytes, making each estimate 1,048,576× too large and causing the JMX metric to report a wildly inflated compaction backlog. Additionally, when a level is under capacity the subtraction is negative, which should contribute zero pending tasks but instead subtracted from the running total. The fix multiplies the divisor by `1024 * 1024` and wraps the difference in `Math.max(0L, ...)`.

## Example
```java
// BEFORE
n += (SSTableReader.getTotalBytes(sstables) - maxBytesForLevel(i))
     / maxSSTableSizeInMB;   // wrong unit, and can be negative
```
```java
// AFTER
n += Math.max(0L, SSTableReader.getTotalBytes(sstables) - maxBytesForLevel(i))
     / (maxSSTableSizeInMB * 1024 * 1024);  // correct bytes divisor
```
