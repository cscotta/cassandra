# Bug 1209: Wrong Error Message When Creating Index on Compact Value Column

**Commit**: 54e58d8c52 · **Ticket**: CASSANDRA-9527
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateIndexStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CreateIndexStatement.validate()` used a single condition to reject secondary index creation on unsupported column types in COMPACT STORAGE tables. The condition `cd.type != ColumnDefinition.Type.REGULAR` was true for `PARTITION_KEY`, `CLUSTERING_KEY`, *and* `COMPACT_VALUE` columns alike, so all three triggered the same error message ("Secondary indexes are not supported on PRIMARY KEY columns in COMPACT STORAGE tables"). When the target column was a `COMPACT_VALUE` column — the implicit value column of a compact table with clustering columns — the message was factually incorrect: the column is not a primary key column, yet that message was shown. The fix splits the check into two separate guards with distinct, accurate messages.

### Before / After

```java
// BEFORE
if ((cfm.getCfDef().isCompact || !cfm.getCfDef().isComposite) && cd.type != ColumnDefinition.Type.REGULAR)
    throw new InvalidRequestException(
        "Secondary indexes are not supported on PRIMARY KEY columns in COMPACT STORAGE tables");
```

```java
// AFTER
if ((cfm.getCfDef().isCompact || !cfm.getCfDef().isComposite)
        && (cd.type == ColumnDefinition.Type.PARTITION_KEY || cd.type == ColumnDefinition.Type.CLUSTERING_KEY))
    throw new InvalidRequestException(
        "Secondary indexes are not supported on PRIMARY KEY columns in COMPACT STORAGE tables");

if (cd.type == ColumnDefinition.Type.COMPACT_VALUE)
    throw new InvalidRequestException(
        "Secondary indexes are not supported on COMPACT STORAGE tables that have clustering columns");
```

---

## How to Spot

Any validation that lumps multiple distinct column kinds into a single catch-all negative (`!= REGULAR`) and emits a message tailored to only one of those kinds will produce misleading errors for the others. Reviewers should check that each error message is achievable only on the path its text describes, and that a negative type test is not silently covering additional types.

---

## Test

New `CreateIndexTest` covers: attempting to index partition key and clustering key columns on a compact table (expects the PRIMARY KEY message), attempting to index the compact value column (expects the new COMPACT_VALUE message), and successfully creating and querying an index on a compact table without clustering columns.
