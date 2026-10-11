# Bug 2094: CQL3 UPDATE Allows Direct Assignment to Counter Columns

**Commit**: 7c920b0bb8 · **Ticket**: CASSANDRA-5300
**Subsystem**: src/java/org/apache/cassandra/cql3/Operation.java
**Category**: Missing null / bounds check

---

## Root Cause

The CQL3 `UPDATE` statement handler did not reject `SET col = <value>` operations when the target column was a counter type. Counters must only be mutated via increment/decrement (`col = col + n`). Allowing a direct assignment bypassed the counter semantics enforced elsewhere, potentially corrupting counter state or causing an inconsistent mutation to be applied to replicas.

### Before / After

```java
// BEFORE — no check, counter columns accepted as plain setters
Term v = value.prepare(receiver);

if (!(receiver.type instanceof CollectionType))
    return new Constants.Setter(...);
```

```java
// AFTER — explicit guard added before setter construction
Term v = value.prepare(receiver);

if (receiver.type instanceof CounterColumnType)
    throw new InvalidRequestException(String.format(
        "Cannot set the value of counter column %s (counters can only be incremented/decremented, not set)", receiver));

if (!(receiver.type instanceof CollectionType))
    return new Constants.Setter(...);
```

---

## How to Spot

When a query path constructs a mutation operation, confirm that all special column types (counters, collections, frozen types) are explicitly validated or routed before a generic operation object is returned. Missing a type-specific guard here silently falls through to the wrong operation.

---

## Test

No test added.
