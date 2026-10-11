# Bug 1973: Misleading Error Message for IN Operator on Non-Partition-Key Column

**Commit**: 25a46eae5d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

The error message thrown when a user applied the `IN` operator to a non-partition-key column claimed the column was "not part of the PRIMARY KEY", which is inaccurate — clustering columns are part of the primary key and can use `IN` in some contexts. The actual restriction is that `IN` is only disallowed on columns not part of the *partition* key when the column is being considered for a secondary index query. The message was corrected to say "not part of the partition key" so users are not misled.

### Before / After

```java
// BEFORE - message incorrectly says PRIMARY KEY
throw new InvalidRequestException("Cannot use IN operator on column not part of the PRIMARY KEY");
```

```java
// AFTER - message accurately identifies the partition key constraint
throw new InvalidRequestException("Cannot use IN operator on column not part of the partition key");
```

---

## How to Spot

Exception messages in query validation code should precisely state which structural element (partition key, clustering key, primary key) causes the restriction. Overly broad terms like "PRIMARY KEY" can mislead users who have clustering columns that legitimately support `IN`.

---

## Test

No test added.
