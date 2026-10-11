# Bug 1549: Fix ReversedType(DateType) Mapping to Native Protocol

**Commit**: ff211a4c44 · **Ticket**: CASSANDRA-7576
**Subsystem**: src/java/org/apache/cassandra/transport/DataType.java
**Category**: Logic error in condition

---

## Root Cause

Same `else if` logic error as CASSANDRA-7576 applied to the 2.0 branch. In `DataType.fromType()`, the `DateType → TimestampType` mapping was inside an `else if` block that was skipped whenever the `ReversedType` branch executed first, causing `ReversedType(DateType)` columns to be reported with an incorrect type to native protocol clients.

### Before / After

```java
// BEFORE
if (type instanceof ReversedType)
    type = ((ReversedType)type).baseType;
// For compatibility sake, we still return DateType as the timestamp type in resultSet metadata (#5723)
else if (type instanceof DateType)
    type = TimestampType.instance;
```

```java
// AFTER
if (type instanceof ReversedType)
    type = ((ReversedType)type).baseType;

// For compatibility sake, we still return DateType as the timestamp type in resultSet metadata (#5723)
if (type instanceof DateType)
    type = TimestampType.instance;
```

---

## How to Spot

When a sequence of type transformations must each be independently applied, using `else if` creates a mutual-exclusion that silently skips later transformations. Whenever two conditions can both be true after the first transformation (here: unwrapping ReversedType can expose DateType), they must be separate `if` statements.

---

## Test

No test added.
