# Bug 0393: ExpressionUtil.selectsPartitions throws exception for unpartitioned tables

**Commit**: 865b60d473d4a43c5dc623fb84fd542fc556fdd9 · **Ticket**: #15243

## Detection Rule

When a method projects an expression against a `PartitionSpec` to compare inclusive and strict projections, verify that the method guards against an unpartitioned spec before calling `Projections.inclusive` or `Projections.strict`, since those projectors may throw or return incompatible results for a spec with no fields.

## Code Smell

A public utility method that calls `Projections.inclusive(spec, ...)` and `Projections.strict(spec, ...)` with no preceding `spec.isUnpartitioned()` guard, combined with a caller that separately short-circuits on `unpartitioned()` rather than delegating the check into the utility itself.

## Tags

- scope: project-specific
- category: missing-null-check
- code-pattern: missing-guard-before-projection
- severity: crash
- subsystem: core/expressions, spark/scan

## Root Cause

`ExpressionUtil.selectsPartitions` directly invoked `Projections.inclusive` and `Projections.strict` without checking whether the spec was unpartitioned. For an unpartitioned spec those projectors can throw, because there are no partition fields to project against. The guard lived only in `SparkScanBuilder.unpartitioned()`, so any other caller of the utility method that passed an unpartitioned spec would crash. Moving the guard into the utility method fixes all callers at once and removes the duplicated check in `SparkScanBuilder`.

## Example

```java
// BEFORE
public static boolean selectsPartitions(
    Expression expr, PartitionSpec spec, boolean caseSensitive) {
  return equivalent(
      Projections.inclusive(spec, caseSensitive).project(expr),
      Projections.strict(spec, caseSensitive).project(expr),
      spec.schema(),
      caseSensitive);
}

// SparkScanBuilder caller had to guard separately:
if (expr == null
    || unpartitioned()
    || !ExpressionUtil.selectsPartitions(expr, table, caseSensitive)) {
  postScanFilters.add(predicate);
}
```

```java
// AFTER
public static boolean selectsPartitions(
    Expression expr, PartitionSpec spec, boolean caseSensitive) {
  if (spec.isUnpartitioned()) {
    return false;
  }
  return equivalent(
      Projections.inclusive(spec, caseSensitive).project(expr),
      Projections.strict(spec, caseSensitive).project(expr),
      spec.schema(),
      caseSensitive);
}

// SparkScanBuilder caller simplified:
if (expr == null || !ExpressionUtil.selectsPartitions(expr, table, caseSensitive)) {
  postScanFilters.add(predicate);
}
```
