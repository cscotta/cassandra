# Bug 3084: Range.differenceToFetch produces wrong results for wrap-around and partial overlap cases

**Commit**: ea5a3d15f0 · **Ticket**: CASSANDRA-3084

## Detection Rule
When implementing set-difference on token ranges that may wrap around the ring, use intersection-based subtraction rather than manual case analysis on endpoint comparisons.

## Code Smell
A `differenceToFetch` implementation that contains manual `comparisonAC` branches comparing left/right endpoints without using `intersectionWith()` will miss wrap-around edge cases and produce incorrect range differences.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: manual-range-arithmetic-misses-wraparound
- severity: silent-wrong-result
- subsystem: streaming

## Root Cause
The original `Range.differenceToFetch` used a series of endpoint comparisons (`comparisonAC > 0`, `comparisonAC == 0`, etc.) to compute the set difference between two token ranges. This ad-hoc approach failed to handle wrap-around ranges and certain partial-overlap cases. The fix replaces the entire implementation with one based on `intersectionWith()` followed by `subtractContained()`, which correctly handles all cases including two-segment intersections produced by wrapping ranges.

## Example
```java
// BEFORE (manual endpoint comparisons, misses edge cases)
int comparisonAC = Range.compare(left, rhs.left);
if (comparisonAC == 0) {
    if (Range.compare(right, rhs.right) < 0)
        difference.add(new Range(right, rhs.right));
} else if (comparisonAC > 0) {
    difference.add(new Range(rhs.left, left));
    if (Range.compare(rhs.right, right) > 0)
        difference.add(new Range(rhs.right, right));
} else { ... }
```
```java
// AFTER (intersection-based subtraction)
Set<Range> intersectionSet = this.intersectionWith(rhs);
if (intersectionSet.isEmpty()) {
    result = new HashSet<Range>();
    result.add(rhs);
} else {
    Range[] intersections = ...;
    result = new HashSet<Range>(rhs.subtractContained(intersections[0]));
    // handle two-segment case for wrap-around
}
```
