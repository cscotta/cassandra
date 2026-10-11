# Bug 0597: NOT IN and != predicates skip pruning single-value data files

**Commit**: bde85b0c347692cd1d2b322f6ab970583e8e3a77 · **Ticket**: #14593

## Detection Rule

When `InclusiveMetricsEvaluator.notEq` or `notIn` unconditionally returns `ROWS_MIGHT_MATCH`, verify whether the file's column bounds satisfy `lower == upper` with no nulls and no NaN values — if so, the exact single value is known and files whose value matches the predicate's exclusion set can be safely pruned.

## Code Smell

`notEq` and `notIn` methods in `InclusiveMetricsEvaluator` that contain only a comment stating "bounds are not necessarily min or max" followed by an unconditional `return ROWS_MIGHT_MATCH`, with no check for the degenerate case where `lowerBound == upperBound`.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: sentinel-not-handled
- severity: performance
- subsystem: api/expressions/InclusiveMetricsEvaluator

## Root Cause

`InclusiveMetricsEvaluator.notEq` and `notIn` always returned `ROWS_MIGHT_MATCH` because column bounds are not guaranteed to be exact min/max values, so general pruning is unsafe. However, when a column's lower bound equals its upper bound and the column has no nulls and no NaN values, every row in the file contains exactly that one value. In this case the file can be safely skipped if that value appears in the exclusion set. The `uniqueValue()` helper added by the fix performs this check before falling through to `ROWS_MIGHT_MATCH`.

## Example

```java
// BEFORE
public <T> Boolean notEq(Bound<T> term, Literal<T> lit) {
    // bounds not necessarily min/max; cannot prune
    return ROWS_MIGHT_MATCH;
}
```

```java
// AFTER
public <T> Boolean notEq(Bound<T> term, Literal<T> lit) {
    T value = uniqueValue(term);
    if (value != null && lit.comparator().compare(value, lit.value()) == 0) {
        return ROWS_CANNOT_MATCH;
    }
    return ROWS_MIGHT_MATCH;
}

private <T> T uniqueValue(Bound<T> term) {
    // non-null only when lower == upper, no nulls, no NaN
    ...
}
```
