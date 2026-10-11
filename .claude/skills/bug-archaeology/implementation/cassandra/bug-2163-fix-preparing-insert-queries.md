# Bug 2163: Preparing CQL3 INSERT queries rejects valid PREPARED operations on key columns

**Commit**: 25d41ceb05 · **Ticket**: CASSANDRA-5016
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/UpdateStatement.java
**Category**: Logic error in condition

---

## Root Cause

During `prepare()` for an INSERT statement, the code validated each column's operation type and rejected anything that was not `Operation.Type.COLUMN` for key and column-alias positions. However, when a query is prepared with bind markers, the parsed operation is a `PreparedOperation` with type `PREPARED`, not `COLUMN`. The guard therefore threw an `InvalidRequestException` ("not a collection type") for every valid prepared INSERT, even though `PREPARED` operations are perfectly legal on key columns.

### Before / After

```java
// BEFORE - PREPARED type rejected for key/column-alias positions
if (operation.getType() != Operation.Type.COLUMN)
    throw new InvalidRequestException(
        String.format("Invalid definition for %s, not a collection type", name));
```

```java
// AFTER - PREPARED type also accepted
if (operation.getType() != Operation.Type.COLUMN
        && operation.getType() != Operation.Type.PREPARED)
    throw new InvalidRequestException(
        String.format("Invalid definition for %s, not a collection type", name));
```

---

## How to Spot

Whenever an operation-type check uses an equality/inequality against a single type constant, verify that the `PREPARED` variant is accounted for. Prepared statements wrap the same semantic operation in a different `Operation.Type` enum value, so exclusive checks will always break the prepared path.

---

## Test

No new test added in this commit.
