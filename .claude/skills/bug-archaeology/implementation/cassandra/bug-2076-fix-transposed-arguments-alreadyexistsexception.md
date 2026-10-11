# Bug 2076: Transposed keyspace/columnfamily Arguments in AlreadyExistsException

**Commit**: c1332efe1c · **Ticket**: CASSANDRA-5362
**Subsystem**: src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Logic error in condition

---

## Root Cause

When a `CREATE TABLE` was rejected because the column family already existed, `MigrationManager` constructed an `AlreadyExistsException` with the arguments in the wrong order: it passed `(cfName, ksName)` but the constructor expected `(ksName, cfName)`. The result was an error message with keyspace and column family names swapped, misleading clients about which object already existed.

### Before / After

```java
// BEFORE
throw new AlreadyExistsException(cfm.cfName, cfm.ksName);
```

```java
// AFTER
throw new AlreadyExistsException(cfm.ksName, cfm.cfName);
```

---

## How to Spot

When reviewing exception construction at the call site, cross-check the argument order against the constructor's parameter names. Arguments of the same type that represent logically distinct concepts (keyspace vs. column family name) are invisible to the type checker and can be silently transposed.

---

## Test

No test added.
