# Bug 0040: notStartsWith in StrictMetricsEvaluator always returns ROWS_MIGHT_NOT_MATCH

**Commit**: 1d5463f687db2a9a80e1a35ac09f36b7e4e8cbeb · **Ticket**: #15883

## Detection Rule

When a metrics evaluator's strict variant implements a predicate, verify it actually uses column lower/upper bounds to prove all rows must satisfy the predicate, rather than unconditionally returning the conservative "might not match" answer.

## Code Smell

A predicate handler in `StrictMetricsEvaluator` whose entire body is `return ROWS_MIGHT_NOT_MATCH;` along with a TODO comment acknowledging that bounds-based pruning could be done, while the corresponding `InclusiveMetricsEvaluator` already checks bounds for the same predicate.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-bounds-check
- severity: performance
- subsystem: api/expressions

## Root Cause

The `notStartsWith` method in `StrictMetricsEvaluator.MetricsEvalVisitor` was a stub with a TODO comment and returned `ROWS_MIGHT_NOT_MATCH` unconditionally. When the column's entire value range lies strictly outside the prefix — either all values sort above the prefix or all values sort below it — every row must satisfy `notStartsWith`, allowing safe file skipping. The missing logic forced every file containing a string column to be read during planning for `notStartsWith` predicates, even when bounds made the result obvious.

## Example

```java
// BEFORE
@Override
public <T> Boolean notStartsWith(BoundReference<T> ref, Literal<T> lit) {
  // TODO: Handle cases that definitely cannot match, such as notStartsWith("x")
  // when the bounds are ["a", "b"].
  return ROWS_MIGHT_NOT_MATCH;
}
```

```java
// AFTER
@Override
public <T> Boolean notStartsWith(BoundReference<T> ref, Literal<T> lit) {
  int id = ref.fieldId();
  if (isNestedColumn(id)) return ROWS_MIGHT_NOT_MATCH;
  if (containsNullsOnly(id)) return ROWS_MUST_MATCH;
  String prefix = (String) lit.value();
  Comparator<CharSequence> cmp = Comparators.charSequences();
  if (lowerBounds != null && lowerBounds.containsKey(id)) {
    CharSequence lower = Conversions.fromByteBuffer(ref.type(), lowerBounds.get(id));
    int len = Math.min(prefix.length(), lower.length());
    if (cmp.compare(lower.subSequence(0, len), prefix) > 0) return ROWS_MUST_MATCH;
  }
  if (upperBounds != null && upperBounds.containsKey(id)) {
    CharSequence upper = Conversions.fromByteBuffer(ref.type(), upperBounds.get(id));
    int len = Math.min(prefix.length(), upper.length());
    if (cmp.compare(upper.subSequence(0, len), prefix) < 0) return ROWS_MUST_MATCH;
  }
  return ROWS_MIGHT_NOT_MATCH;
}
```
