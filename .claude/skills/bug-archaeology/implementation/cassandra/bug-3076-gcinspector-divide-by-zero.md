# Bug 3076: GCInspector divide-by-zero when collection count is zero

**Commit**: 51d20ba33e · **Ticket**: CASSANDRA-3076

## Detection Rule
When computing a rate or duration by dividing by a counter that may be zero on first observation, add a zero-check guard before the division.

## Code Smell
Code that uses `gc.getCollectionCount()` as a divisor (or computes `delta / count`) without checking `count == 0` will throw `ArithmeticException` on the first GC inspection run before any collections have occurred.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: missing-zero-guard-before-divide
- severity: crash
- subsystem: config

## Root Cause
`GCInspector` computed per-collection GC statistics by dividing elapsed time by the collection count. If `getCollectionCount()` returned 0 (before any GC had occurred, or on first call), the division threw an `ArithmeticException`. The fix adds a `continue` guard that skips the calculation entirely when `count == 0`.

## Example
```java
// BEFORE
Long previousCount = gccounts.get(gc.getName());
Long count = gc.getCollectionCount();
if (previousCount == null)
    previousCount = 0L;
gccounts.put(gc.getName(), count);
// ... uses count as divisor below
```
```java
// AFTER
Long previousCount = gccounts.get(gc.getName());
Long count = gc.getCollectionCount();
if (count == 0)
    continue;
if (previousCount == null)
    previousCount = 0L;
gccounts.put(gc.getName(), count);
```
