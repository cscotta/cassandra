# Bug 2166: CQL3 CREATE TABLE with COMPACT STORAGE silently allows collection columns

**Commit**: f5a4a1dfc5 · **Ticket**: CASSANDRA-4990
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateColumnFamilyStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

`CreateColumnFamilyStatement` validated many constraints for `COMPACT STORAGE` tables (e.g. requiring non-empty non-PK column definitions) but never checked whether the statement also contained collection-type columns. Since compact storage uses a fundamentally different on-disk layout that does not support the composite column-name encoding required by collections, allowing this combination would produce a table that appears to be created successfully but whose collection columns could not be stored or queried correctly. The fix inserts an explicit guard that rejects the combination before any schema is written.

### Before / After

```java
// BEFORE - no check for collections in compact storage path
if (stmt.columns.isEmpty())
    throw new InvalidRequestException(
        "No definition found that is not part of the PRIMARY KEY");
stmt.comparator = CFDefinition.definitionType;
```

```java
// AFTER - collection columns rejected before layout is finalized
if (stmt.columns.isEmpty())
    throw new InvalidRequestException(
        "No definition found that is not part of the PRIMARY KEY");
if (definedCollections != null)
    throw new InvalidRequestException(
        "Collection types are not supported with COMPACT STORAGE");
stmt.comparator = CFDefinition.definitionType;
```

---

## How to Spot

When a storage format has a structural incompatibility with a schema feature, the validation for that incompatibility should be co-located with the other validations for that storage format. If you see a block that validates compact-storage constraints without a collections check, assume the check is missing.

---

## Test

No new test added in this commit.
