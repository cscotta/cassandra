# Bug 1381: Secondary Index Validation Allows PRIMARY KEY Columns in Non-Composite COMPACT STORAGE Tables

**Commit**: 345e69e1c1 · **Ticket**: CASSANDRA-8156
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateIndexStatement.java
**Category**: Logic error in condition

---

## Root Cause

The validation guard in `CreateIndexStatement.validate()` that rejects secondary indexes on PRIMARY KEY columns in COMPACT STORAGE tables checked only `cfm.getCfDef().isCompact`. This condition missed the case of non-composite (single-clustering-column) COMPACT tables: `isCompact` can be false for those schemas while the table is still incompatible with secondary indexes on PRIMARY KEY columns. An index created on such a column would be accepted at DDL time but behave incorrectly at query time because the composite index machinery expected a composite column definition that was not present.

### Before / After

```java
// BEFORE: only checks isCompact, misses non-composite case
if (cfm.getCfDef().isCompact && cd.type != ColumnDefinition.Type.REGULAR)
    throw new InvalidRequestException(...);
```

```java
// AFTER: rejects when compact OR non-composite
if ((cfm.getCfDef().isCompact || !cfm.getCfDef().isComposite) && cd.type != ColumnDefinition.Type.REGULAR)
    throw new InvalidRequestException("Secondary indexes are not supported on PRIMARY KEY columns in COMPACT STORAGE tables");
```

---

## How to Spot

When a validation gate encodes knowledge about a table's internal layout, every independent structural property that makes the operation unsupported must appear in the condition. A reviewer should enumerate all table variants the code path will encounter (compact, non-composite, static, counter, etc.) and verify the guard rejects each invalid combination, not just the most common one. Missing a disjunct here results in the system accepting invalid DDL silently and failing at query execution instead.

---

## Test

No new test added in this commit; covered by existing integration tests for `CREATE INDEX`.
