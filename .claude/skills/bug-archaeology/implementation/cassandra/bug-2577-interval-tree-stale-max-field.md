# Bug 2577: IntervalTree uses stale public `max` field instead of computed maximum

**Commit**: 44a566d32f · **Ticket**: CASSANDRA-3145

## Detection Rule
When an `IntervalTree` stores its maximum endpoint as a public mutable field populated from a pre-sorted list, and the list is no longer sorted when `IntervalTree` is constructed, the `max` field contains a wrong value; callers that use `view.intervalTree.max` as the stop bound for a range scan will miss SSTables.

## Code Smell
`view.intervalTree.max` is a public field accessed directly by `ColumnFamilyStore`; `IntervalTree` computes it from `intervals.get(intervals.size() - 1).max`, which assumes the list is sorted by interval max, but the pre-sort step was removed from `buildIntervalTree`.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: stale-mutable-public-field
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`IntervalTree` stored `min` and `max` as public fields set from the first and last elements of what was assumed to be a sorted interval list. `DataTracker.buildIntervalTree` was refactored to remove the pre-sort, making the field values arbitrary. Additionally, `ColumnFamilyStore` accessed the field directly (`view.intervalTree.max`) rather than via a method, bypassing the tree's internal node structure. The fix removes the mutable fields, computes min/max within each `IntervalNode` during construction (`findMinMedianMax`), and exposes them via `max()` and `min()` accessor methods.

## Example
```java
// BEFORE: field populated from (no longer) sorted list
public Comparable max = null; // stale after sort removed
...
Comparable stopInTree = stopAt.isEmpty()
    ? view.intervalTree.max   // wrong value
    : stopAt;
```
```java
// AFTER: computed from tree nodes
public Comparable max() { return head.v_max; }
...
Comparable stopInTree = stopAt.isEmpty()
    ? view.intervalTree.max() // correct
    : stopAt;
```
