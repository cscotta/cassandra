# Bug 2510: Hadoop record reader stops after first all-ghost batch and uses wrong predicate-empty check

**Commit**: cc0ad2a0c8 · **Ticket**: CASSANDRA-3450

## Detection Rule
When a Hadoop input format filters rows after fetching a batch and the filtered result can be empty, the fetch loop must retry rather than returning, and predicate-emptiness must use `||` (either bound absent) not `&&` (both absent).

## Code Smell
`isPredicateEmpty` checked `getStart() != null && getFinish() != null` to conclude "not empty", so any range with only one non-null bound was incorrectly treated as an empty predicate and ghost rows were removed when they should not have been. After filtering a batch to empty, the code returned immediately instead of fetching the next batch.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-logical-operator-and-vs-or
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
Two independent bugs: (1) `isPredicateEmpty` used `&&` — requiring both start *and* finish to be non-null — so a predicate with only one bound set was considered empty and ghost rows were erroneously stripped. (2) After removing all empty rows from a batch the code fell through and returned without fetching the next batch, causing the MapReduce job to silently process fewer rows than exist. The fix wraps the fetch in a `while (rows == null || rows.isEmpty())` loop and changes `&&` to `||`.

## Example
```java
// BEFORE (wrong operator)
if (predicate.getSlice_range().getStart() != null
        && predicate.getSlice_range().getFinish() != null)
    return false;   // treated as non-empty only when BOTH bounds set
```
```java
// AFTER
if (predicate.getSlice_range().getStart() != null
        || predicate.getSlice_range().getFinish() != null)
    return false;   // non-empty if EITHER bound is set
```
