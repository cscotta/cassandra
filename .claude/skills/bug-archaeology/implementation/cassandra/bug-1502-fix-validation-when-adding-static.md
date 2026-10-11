# Bug 1502: Fix validation when adding static columns

**Commit**: 73b02d67c1 · **Ticket**: CASSANDRA-7730
**Subsystem**: cql3/statements/AlterTableStatement
**Category**: Missing null / bounds check

---

## Root Cause

`AlterTableStatement` validated that static columns cannot be added to `COMPACT STORAGE` tables, but did not check whether the table actually has clustering columns. Static columns are only meaningful (and internally consistent) when at least one clustering column exists; without a clustering column every row is effectively a single static cell and the concept is undefined. Adding a static column to a table with no clustering columns could corrupt the schema. The fix adds the missing guard that rejects the DDL statement with a clear error message.

### Before / After

```java
// BEFORE
if (isStatic && !cfDef.isComposite)
    throw new InvalidRequestException("Static columns are not allowed in COMPACT STORAGE tables");
```

```java
// AFTER
if (isStatic)
{
    if (!cfDef.isComposite)
        throw new InvalidRequestException("Static columns are not allowed in COMPACT STORAGE tables");
    if (cfDef.clusteringColumns().isEmpty())
        throw new InvalidRequestException(
            "Static columns are only useful (and thus allowed) if the table has at least one clustering column");
}
```

---

## How to Spot

Schema-altering DDL paths should validate all preconditions for the feature being added, not just the most obvious one. When a feature requires another feature to make sense (static columns require clustering columns), the dependency should be explicitly checked.

---

## Test

No test added.
