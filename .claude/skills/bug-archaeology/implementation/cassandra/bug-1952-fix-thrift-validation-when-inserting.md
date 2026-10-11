# Bug 1952: Thrift validation used wrong column map for CQL3 tables

**Commit**: 7a300c2a5e · **Ticket**: CASSANDRA-5138
**Subsystem**: src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Logic error in condition

---

## Root Cause

When Thrift validates a cell being inserted into a CQL3 table, it needs to verify that the CQL3 column component in the composite cell name corresponds to a real CQL3 column. The code was looking up the column identifier in `cfDef.columns` (which holds only primary key components) instead of `cfDef.metadata` (which holds the non-key CQL3 column definitions). Any valid non-primary-key column would fail the lookup and throw an `InvalidRequestException`, making Thrift writes into CQL3 tables effectively broken.

### Before / After

```java
// BEFORE
if (cfDef.columns.get(columnId) == null)
    throw new org.apache.cassandra.exceptions.InvalidRequestException(...);
```

```java
// AFTER
if (cfDef.metadata.get(columnId) == null)
    throw new org.apache.cassandra.exceptions.InvalidRequestException(...);
```

---

## How to Spot

When column validation logic uses a `CFDefinition`, check which map is being queried: `columns` holds primary key columns, `metadata` holds regular (non-key) CQL3 columns. A lookup in the wrong map will produce false negatives or false positives for any non-key column.

---

## Test

No test added.
