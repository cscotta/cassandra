# Bug 2570: Node move streams data to endpoints that already own the range

**Commit**: 1f7ff16210 · **Ticket**: CASSANDRA-3041

## Detection Rule
When computing which endpoints need data streamed to them during a move or decommission, using a mutable collection and calling `removeAll` instead of set difference can fail silently if the collection type does not support `removeAll` correctly, or the wrong metadata snapshot is used.

## Code Smell
`newEndpoints.removeAll(currentEndpoints)` is called where `newEndpoints` is a `Collection` returned by `calculateNaturalEndpoints`, which may return an unmodifiable or non-set collection; the mutation either throws or produces incorrect results. Additionally, for move operations the `currentEndpoints` is computed from `tokenMetaClone` (already-moved state) rather than `tokenMetadata_` (pre-move state).

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-set-difference-baseline
- severity: performance
- subsystem: streaming

## Root Cause
During decommission, `newEndpoints.removeAll(currentEndpoints)` was called on a collection that might not support mutation. During move, `calculateNaturalEndpoints` was called with `tokenMetaClone` for both old and new endpoints, making them identical and the difference empty — or conversely always non-empty. The fix uses `ImmutableSet.copyOf()` on both sets and `Sets.difference()` for a correct immutable set subtraction, computing `currentEndpoints` from the live `tokenMetadata_` and `newEndpoints` from the post-change clone.

## Example
```java
// BEFORE
Collection<InetAddress> currentEndpoints =
    strategy.calculateNaturalEndpoints(range.right, tm);
Collection<InetAddress> newEndpoints =
    strategy.calculateNaturalEndpoints(range.right, allLeftMetadata);
newEndpoints.removeAll(currentEndpoints); // may fail or be wrong
pendingRanges.putAll(range, newEndpoints);
```
```java
// AFTER
Set<InetAddress> currentEndpoints = ImmutableSet.copyOf(
    strategy.calculateNaturalEndpoints(range.right, tm));
Set<InetAddress> newEndpoints = ImmutableSet.copyOf(
    strategy.calculateNaturalEndpoints(range.right, allLeftMetadata));
pendingRanges.putAll(range, Sets.difference(newEndpoints, currentEndpoints));
```
