# Bug 1914: Thrift Validation Rejects CQL3 Row Markers

**Commit**: ba59423b69 · **Ticket**: CASSANDRA-6081
**Subsystem**: src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Missing null / bounds check

---

## Root Cause

CQL3 tables store an empty-named "row marker" column to signal that a row exists even when all non-primary-key columns are null. When Thrift validation iterated over composite column names to verify that each CQL3 column component matched a declared column definition, it attempted to look up a `ColumnIdentifier` for the empty-byte-buffer component. Because no column definition has an empty name, the lookup returned null and the code threw an `InvalidRequestException`, incorrectly rejecting valid CQL3 data accessed via Thrift.

The fix adds an early `continue` when the CQL3 column-name component has no remaining bytes, treating the empty component as the well-known row marker.

### Before / After

```java
// BEFORE
ByteBuffer CQL3ColumnName = components[columnIndex];
ColumnIdentifier columnId = new ColumnIdentifier(CQL3ColumnName, ...);
if (cfDef.metadata.get(columnId) == null)
    throw new InvalidRequestException(...);
```

```java
// AFTER
ByteBuffer CQL3ColumnName = components[columnIndex];
if (!CQL3ColumnName.hasRemaining())
    continue; // Row marker, ok

ColumnIdentifier columnId = new ColumnIdentifier(CQL3ColumnName, ...);
if (cfDef.metadata.get(columnId) == null)
    throw new InvalidRequestException(...);
```

---

## How to Spot

When validating composite column components against a schema, always account for the "row marker" convention (empty last component). Any loop that assumes every component maps to a named column definition will fail on CQL3 tables that use row markers.

---

## Test

No test added.
