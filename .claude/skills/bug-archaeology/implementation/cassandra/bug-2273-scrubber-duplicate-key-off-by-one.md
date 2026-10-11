# Bug 2273: Scrubber Misses Duplicate-Key Out-of-Order Rows (> vs >=)

**Commit**: ce368f812c · **Ticket**: CASSANDRA-4411

## Detection Rule

When a comparator check is meant to detect "not strictly increasing" order, verify the operator is `>= 0` (catches equal keys too) rather than `> 0` (which silently passes duplicate keys as in-order).

## Code Smell

`acrComparator.compare(prevRow, compactedRow) > 0` passes silently when two consecutive rows have the same key (compare returns 0), allowing duplicate-key rows to be treated as correctly ordered.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: strict-comparison-misses-equals
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

The offline scrubber detected out-of-order rows by checking `compare(prevRow, compactedRow) > 0`, which only triggers when the previous row is strictly greater than the current one. When two consecutive rows had identical keys (`compare` returns 0), the check passed, leaving duplicate keys uncaught. The fix changes `> 0` to `>= 0` so that equal (duplicate) keys are also flagged as out-of-order and moved to the out-of-order set.

## Example

```java
// BEFORE
if (prevRow != null && acrComparator.compare(prevRow, compactedRow) > 0)
{
    outOfOrderRows.add(compactedRow);
    outputHandler.warn(...);
}
```

```java
// AFTER
if (prevRow != null && acrComparator.compare(prevRow, compactedRow) >= 0)
{
    outOfOrderRows.add(compactedRow);
    outputHandler.warn(...);
}
```
