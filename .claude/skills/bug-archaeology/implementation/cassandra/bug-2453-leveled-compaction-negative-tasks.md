# Bug 2453: Leveled compaction reports negative pending task count

**Commit**: 10a8f67783 · **Ticket**: CASSANDRA-3693

## Detection Rule

When computing a numeric estimate by subtracting a `long` from another `long` and accumulating the result into an `int`, check for integer overflow and ensure intermediate computations do not wrap around to negative values.

## Code Smell

`int n = 0; n += Math.max(0L, bytes - max) / unit` where `bytes - max` can exceed `Integer.MAX_VALUE`; accumulating into an `int` silently overflows to a negative count visible in JMX.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: int-overflow-accumulation
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

`LeveledManifest.getEstimatedTasks()` accumulated per-level task estimates into a plain `int`. When total bytes across levels was large, `(SSTableReader.getTotalBytes(sstables) - maxBytesForLevel(i))` produced a value exceeding `Integer.MAX_VALUE / maxSSTableSizeInMB`, causing the `int` to overflow and become negative. This showed as a negative pending-compaction-tasks counter in JMX. The fix accumulates into a `long` and uses `Ints.checkedCast()` to catch overflow at conversion time. A related fix changes the return type of `maxBytesForLevel` from `double` to `long` to avoid floating-point precision issues.

## Example

```java
// BEFORE – int overflow when bytes is large
int n = 0;
for (int i = ...; i >= 0; i--)
    n += Math.max(0L, getTotalBytes(sstables) - maxBytesForLevel(i)) / unitBytes;
return n; // can be negative

// AFTER
long tasks = 0;
for (int i = ...; i >= 0; i--) {
    long n = Math.max(0L, getTotalBytes(sstables) - maxBytesForLevel(i)) / unitBytes;
    tasks += n;
}
return Ints.checkedCast(tasks);
```
