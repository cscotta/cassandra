# Bug 1750: Fix Assertion Error in ALTER TYPE RENAME

**Commit**: e30f11143a · **Ticket**: CASSANDRA-6705
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/, src/java/org/apache/cassandra/db/composites/, src/java/org/apache/cassandra/db/marshal/
**Category**: Logic error in condition

---

## Root Cause

When a user-defined type field referenced by a collection column was renamed via `ALTER TYPE ... RENAME`, the code updated the `ColumnDefinition` but did not propagate the change into the `CellNameType` comparator, which also stores a copy of the collection's type information. The comparator was left referencing the old type, and a subsequent assertion or compatibility check on the comparator's internal map would fail. Additionally, the `addCollection` method silently overwrote existing entries rather than updating them, and `ColumnToCollectionType.isCompatibleWith` had an inverted null check that could produce a `NullPointerException`. The fix renames `addCollection` to `addOrUpdateCollection` (making the intent clear), adds the missing comparator update paths in both `AlterTableStatement` and `AlterTypeStatement`, and corrects the null guard in the compatibility check.

### Before / After

```java
// BEFORE (ColumnToCollectionType.isCompatibleWith)
if (!entry.getValue().isCompatibleWith(defined.get(entry.getKey())))
    return false;
```

```java
// AFTER (ColumnToCollectionType.isCompatibleWith)
CollectionType newType = defined.get(entry.getKey());
if (newType == null || !newType.isCompatibleWith(entry.getValue()))
    return false;
```

---

## How to Spot

When schema objects maintain duplicated representations of the same type information (e.g., both a `ColumnDefinition` list and a comparator map), any mutation must update all copies atomically. During review, search for schema-altering code paths and verify that every representation is kept in sync; a missing update to a secondary copy is a common source of assertion failures and subtle corruption.

---

## Test

No test added.
