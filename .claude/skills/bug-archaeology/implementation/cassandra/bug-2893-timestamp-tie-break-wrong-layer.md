# Bug 2893: Timestamp tie-breaking for tombstones applied in Column not in reconciler

**Commit**: 775293210c · **Ticket**: n/a

## Detection Rule
When tie-breaking logic for equal-timestamp columns exists in two places (a `Column.comparePriority` method and a reconciler), check that the logic is consistent and that the authoritative path (the reconciler) handles all cases correctly.

## Code Smell
`Column.comparePriority` contained inline tombstone-wins-on-equal-timestamp logic. `TimestampReconciler.reconcile` for the `EQUAL` case fell through to `return left` without checking for tombstones or value comparison, so the tie-break was only applied when callers used `comparePriority` directly and not when going through the reconciler.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: tie-break-logic-in-wrong-layer
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
Reconciliation tie-breaking was implemented in `Column.comparePriority` but `TimestampReconciler` did not use it. The fix removes `comparePriority` and adds the correct logic directly into `TimestampReconciler.reconcile` for the `EQUAL` case: tombstone over live, then higher-value wins.

## Example
```java
// BEFORE (TimestampReconciler) — equal timestamps always return left, ignoring tombstones
case EQUAL:
case GREATER_THAN:
    return left;
```
```java
// AFTER — tombstone wins ties, then value comparison
case EQUAL:
    if (left.isMarkedForDelete()) return left;
    if (right.isMarkedForDelete()) return right;
    return FBUtilities.compareByteArrays(left.value(), right.value()) < 0 ? right : left;
case GREATER_THAN:
    return left;
```
