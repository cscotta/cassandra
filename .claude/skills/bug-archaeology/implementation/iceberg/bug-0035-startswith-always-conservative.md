# Bug 0035: startsWith in StrictMetricsEvaluator always returns ROWS_MIGHT_NOT_MATCH

**Commit**: 57b7c2bd564e5dfa8ccb7cb9e6386e7e942bac0f · **Ticket**: #15902

## Detection Rule

When a metrics evaluator's strict variant implements a predicate, verify it actually uses column lower/upper bounds to prove all rows must satisfy the predicate, rather than unconditionally returning the conservative "might not match" answer.

## Code Smell

A predicate handler in `StrictMetricsEvaluator` whose entire body is `return ROWS_MIGHT_NOT_MATCH;` with no bounds inspection, while the corresponding `InclusiveMetricsEvaluator` does examine bounds for the same predicate.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-bounds-check
- severity: performance
- subsystem: api/expressions

## Root Cause

The `startsWith` method in `StrictMetricsEvaluator.MetricsEvalVisitor` was a stub that unconditionally returned `ROWS_MIGHT_NOT_MATCH`, skipping any evaluation. When both the column's lower and upper bounds start with the queried prefix, every value in the file must also start with that prefix, so the evaluator can safely return `ROWS_MUST_MATCH` and allow entire data file skipping during planning. The missing logic caused unnecessary file reads.

## Example

```java
// BEFORE
@Override
public <T> Boolean startsWith(BoundReference<T> ref, Literal<T> lit) {
  return ROWS_MIGHT_NOT_MATCH;
}
```

```java
// AFTER
@Override
public <T> Boolean startsWith(BoundReference<T> ref, Literal<T> lit) {
  int id = ref.fieldId();
  if (isNestedColumn(id) || canContainNulls(id)) {
    return ROWS_MIGHT_NOT_MATCH;
  }
  if (lowerBounds != null && lowerBounds.containsKey(id)
      && upperBounds != null && upperBounds.containsKey(id)) {
    String prefix = (String) lit.value();
    Comparator<CharSequence> cmp = Comparators.charSequences();
    CharSequence lower = Conversions.fromByteBuffer(ref.type(), lowerBounds.get(id));
    CharSequence upper = Conversions.fromByteBuffer(ref.type(), upperBounds.get(id));
    if (lower.length() >= prefix.length() && upper.length() >= prefix.length()
        && cmp.compare(lower.subSequence(0, prefix.length()), prefix) == 0
        && cmp.compare(upper.subSequence(0, prefix.length()), prefix) == 0) {
      return ROWS_MUST_MATCH;
    }
  }
  return ROWS_MIGHT_NOT_MATCH;
}
```
