# Bug 1916: IN Predicate on Non-Primary-Key Columns Silently Accepted With Secondary Indexes

**Commit**: a0abadfdb5 · **Ticket**: CASSANDRA-6050
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/cql3/statements/Restriction.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

CQL3 only supports `IN` predicates on the partition key and the last clustering key column. When a non-primary-key column had a secondary index, the `SELECT` statement parser accepted an `IN` restriction on it without validation, allowing a query to proceed that the execution engine could not handle correctly. The validation that rejects `IN` on non-primary-key columns was missing entirely for the `COLUMN_METADATA` case, so any `IN (...)` predicate against an indexed regular column bypassed the guard.

The fix validates the parsed restriction immediately: if it is an `IN` with potentially more than one value, the query is rejected with a clear error. A single-value `IN` is treated as `EQ` for backward compatibility, so the new `canHaveOnlyOneValue()` method on `Restriction.IN` is used to distinguish that case without needing the bind-variable values.

### Before / After

```java
// BEFORE
case COLUMN_METADATA:
    stmt.metadataRestrictions.put(name,
        updateRestriction(name, stmt.metadataRestrictions.get(name), rel, names));
    break;
```

```java
// AFTER
case COLUMN_METADATA:
    Restriction r = updateRestriction(name, stmt.metadataRestrictions.get(name), rel, names);
    if (r.isIN() && !((Restriction.IN)r).canHaveOnlyOneValue())
        throw new InvalidRequestException(
            String.format("IN predicates on non-primary-key columns (%s) is not yet supported", name));
    stmt.metadataRestrictions.put(name, r);
    break;
```

---

## How to Spot

Any path that parses query restrictions and then routes them based on column kind (partition key vs. clustering key vs. regular column) must include explicit validation for each operator type at each branch. Missing a validation case in one branch leaves an unsupported operator silently accepted.

---

## Test

No test added.
