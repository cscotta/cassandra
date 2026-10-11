# Bug 0474: NOT IN and != predicates fail to prune single-value partition manifests

**Commit**: 157b2488ebfc138370bf482458df96bcdc7245dd · **Ticket**: #15064

## Detection Rule

When a manifest evaluator returns `ROWS_MIGHT_MATCH` for a negation predicate (`notEq`, `notIn`), verify whether the partition field has identical lower and upper bounds with no nulls or NaN values — if it does, the exact value is known and pruning is possible.

## Code Smell

`notEq` and `notIn` methods in `ManifestEvaluator` unconditionally return `ROWS_MIGHT_MATCH` with a comment explaining that bounds aren't exact, without first checking whether `lowerBound == upperBound` (which would mean all partitions share exactly one value).

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: sentinel-not-handled
- severity: performance
- subsystem: api/expressions/ManifestEvaluator

## Root Cause

`ManifestEvaluator.notEq` and `notIn` always returned `ROWS_MIGHT_MATCH` because bounds are not guaranteed to be min/max values. However, when `lowerBound == upperBound` and the field has no nulls or NaN values, the partition contains exactly one known value, making safe pruning possible. The evaluator lacked a `uniqueValue()` helper to detect this case.

## Example

```java
// BEFORE
public <T> Boolean notEq(BoundReference<T> ref, Literal<T> lit) {
    // bounds are not necessarily min/max; cannot prune
    return ROWS_MIGHT_MATCH;
}
```

```java
// AFTER
public <T> Boolean notEq(BoundReference<T> ref, Literal<T> lit) {
    T value = uniqueValue(ref);
    if (value != null && lit.comparator().compare(value, lit.value()) == 0) {
        return ROWS_CANNOT_MATCH;
    }
    return ROWS_MIGHT_MATCH;
}

private <T> T uniqueValue(BoundReference<T> ref) {
    // returns non-null only when lower == upper, no nulls, no NaN
    ...
}
```
