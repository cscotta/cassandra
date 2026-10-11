# Bug 2066: isDead() Parameter Type Uses Interface Instead of Concrete Class After Merge

**Commit**: 0b20e139d6 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/Selection.java
**Category**: Logic error in condition

---

## Root Cause

A merge conflict resolution left the `isDead()` helper method in `Selection.ResultSetBuilder` with the parameter type `IColumn` instead of `Column`. The surrounding code had already been updated to use `Column` (the concrete class that replaced the `IColumn` interface in this branch), so the wrong type caused a compile-time or type-mismatch error that broke the build.

### Before / After

```java
// BEFORE – wrong type from bad merge
private boolean isDead(IColumn c)
{
    return c == null || c.isMarkedForDelete();
}
```

```java
// AFTER – corrected to concrete class
private boolean isDead(Column c)
{
    return c == null || c.isMarkedForDelete();
}
```

---

## How to Spot

After resolving merge conflicts, verify that every parameter and variable type is consistent with the branch being merged into. Using "find all usages" of the old interface name is a quick way to catch surviving references.

---

## Test

No test added.
