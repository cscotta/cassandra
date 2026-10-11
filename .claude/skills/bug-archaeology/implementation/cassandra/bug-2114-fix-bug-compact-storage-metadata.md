# Bug 2114: Wrong condition in COMPACT STORAGE PRIMARY KEY validation allows invalid schemas

**Commit**: be36736d38 · **Ticket**: CASSANDRA-5189
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateColumnFamilyStatement.java
**Category**: Logic error in condition

---

## Root Cause

The condition guarding the "compact storage with column aliases" code path used `stmt.columns.size() <= 1` instead of `!stmt.columnAliases.isEmpty()`. These two conditions select different cases: the old condition entered the block whenever there was at most one non-PK column regardless of whether there were any column aliases (composite PK clustering columns). The result was that the validation logic was applied to the wrong structural cases, allowing table definitions with multiple non-PK columns under COMPACT STORAGE to pass when they should have been rejected, or rejecting valid definitions with no non-PK columns that did have column aliases.

### Before / After

```java
// BEFORE
if (useCompactStorage && stmt.columns.size() <= 1)
{
    if (stmt.columns.isEmpty())
    {
        if (columnAliases.isEmpty())
            throw new InvalidRequestException(...);
        // ...
    }
    else { ... }
}
else
{
    if (useCompactStorage && !columnAliases.isEmpty())
        throw new InvalidRequestException(...);
    // ...
}
```

```java
// AFTER
if (useCompactStorage && !stmt.columnAliases.isEmpty())
{
    if (stmt.columns.isEmpty())
    {
        // The only value we'll insert will be the empty one
        stmt.defaultValidator = BytesType.instance;
        // ...
    }
    else
    {
        if (stmt.columns.size() > 1)
            throw new InvalidRequestException(...);
        // ...
    }
}
else
{
    if (useCompactStorage && stmt.columns.isEmpty())
        throw new InvalidRequestException("COMPACT STORAGE with non-composite PRIMARY KEY require one column not part of the PRIMARY KEY, none given");
    // ...
}
```

---

## How to Spot

When two separate conditions (`columns.size()` and `columnAliases.isEmpty()`) are closely related but structurally different, verify that the outer branch uses the correct discriminator. Review the test matrix of combinations (composite vs non-composite PK, zero vs one vs many non-PK columns) to ensure each is dispatched to the right branch.

---

## Test

No test added.
