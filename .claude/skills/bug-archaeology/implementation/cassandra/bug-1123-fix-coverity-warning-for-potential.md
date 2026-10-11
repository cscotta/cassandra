# Bug 1123: NPE on Null targetColumnNames Before Size Assertion in SchemaKeyspace

**Commit**: 0d7b232bdb · **Ticket**: CASSANDRA-6717
**Subsystem**: schema/SchemaKeyspace
**Category**: Missing null / bounds check

---

## Root Cause

In `SchemaKeyspace.createIndexMetadataFromRow`, the assertion `targetColumnNames.size() == 1` was placed before the null check for `targetColumnNames`. The `Row.getSet()` call can return `null` for absent columns, so the assert would throw a `NullPointerException` rather than an `AssertionError` on rows where the column is missing. Moving the size assertion inside the `if (targetColumnNames != null)` block ensures the null guard executes first.

### Before / After

```java
// BEFORE
Set<String> targetColumnNames = row.getSet("target_columns", UTF8Type.instance);
assert targetType == IndexMetadata.TargetType.COLUMN : "...";
assert targetColumnNames.size() == 1 : "...";  // NPE if null

Set<ColumnIdentifier> targetColumns = new HashSet<>();
if (targetColumnNames != null)
{
    targetColumnNames.forEach(...);
```

```java
// AFTER
Set<String> targetColumnNames = row.getSet("target_columns", UTF8Type.instance);
assert targetType == IndexMetadata.TargetType.COLUMN : "...";

Set<ColumnIdentifier> targetColumns = new HashSet<>();
if (targetColumnNames != null)
{
    assert targetColumnNames.size() == 1 : "...";  // safe
    targetColumnNames.forEach(...);
```

---

## How to Spot

When an assertion or method call appears before a null guard that was clearly intended to protect it, the ordering is wrong. Look for patterns where a variable is dereferenced one or two lines before the `if (x != null)` block that was supposed to be its guard. Static analysis tools (Coverity, SpotBugs) reliably flag this ordering mistake as a potential null dereference.

---

## Test

No test added.
