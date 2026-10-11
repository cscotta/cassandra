# Bug 1634: Fix Broken Build in ModificationStatement

**Commit**: 60dbe8b700 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Logic error in condition

---

## Root Cause

A preceding refactor of `ModificationStatement` updated the CQL composites API — replacing `ColumnNameBuilder` with `Composite`, `List<ByteBuffer> variables` with `QueryOptions`, and `ColumnIdentifier`-based conditions with `ColumnDefinition`-based ones — but left behind stale references, missing constructor parameters, and inconsistent field declarations that caused compilation failures. The `boundTerms` field was declared with `private int` (non-final, set later) but the refactored code expected it to be passed into the constructor and set as `final`, and the `getColumnForCondition` function retained the old `ColumnIdentifier` return type instead of `ColumnDefinition`. The counter deprecation warnings were also replaced with hard errors matching the new 2.1 semantics.

### Before / After

```java
// BEFORE
private int boundTerms;
private final Function<ColumnCondition, ColumnIdentifier> getColumnForCondition = ...;
public ModificationStatement(StatementType type, CFMetaData cfm, Attributes attrs)
public abstract void addUpdateForKey(..., ColumnNameBuilder builder, ...);
```

```java
// AFTER
private final int boundTerms;
private final Function<ColumnCondition, ColumnDefinition> getColumnForCondition = ...;
public ModificationStatement(StatementType type, int boundTerms, CFMetaData cfm, Attributes attrs)
public abstract void addUpdateForKey(..., Composite prefix, ...);
```

---

## How to Spot

Large refactors that change core type signatures (especially abstract method signatures shared by many subclasses) often leave stale import lists and field declarations that compile only because the old symbols are still in scope. Reviewing the full import list and field declarations after an API migration surfaces these residual inconsistencies before they break the build.

---

## Test

No test added.
