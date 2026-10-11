# Bug 0382: SparkStagedScan hashCode uses splitSize twice instead of splitLookback

**Commit**: 71b05af09e1012c38eadd2e9393a517dfaf6ab21 · **Ticket**: #15281

## Detection Rule

When a class implements `hashCode()` by passing a fixed list of fields to `Objects.hash()`, verify that each logically distinct field appears exactly once and that no field name is repeated by accident.

## Code Smell

A `hashCode()` call that passes the same variable (`splitSize`) twice in its argument list where two different variables (`splitSize` and `splitLookback`) should appear — the duplicated argument is visually identical to its neighbour, making the mistake easy to miss in review.

## Tags

- scope: java
- category: wrong-filter-result
- code-pattern: else-if-masks-second
- severity: silent-wrong-result
- subsystem: spark/scan

## Root Cause

`SparkStagedScan.hashCode()` listed `splitSize, splitSize` instead of `splitSize, splitLookback`. Two staged scans that differ only in their lookback value would hash to the same bucket and then be incorrectly treated as equal, preventing Spark from correctly deduplifying or caching distinct scans.

## Example

```java
// BEFORE
public int hashCode() {
    return Objects.hash(
        table().name(), taskSetId, readSchema(), splitSize, splitSize, openFileCost);
    //                                                      ^^^^^^^^^^ should be splitLookback
}
```

```java
// AFTER
public int hashCode() {
    return Objects.hash(
        table().name(), taskSetId, readSchema(), splitSize, splitLookback, openFileCost);
}
```
