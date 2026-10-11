# Bug 1548: Fix ReversedType(DateType) Mapping to Native Protocol

**Commit**: 4f6e108e14 · **Ticket**: CASSANDRA-7576
**Subsystem**: src/java/org/apache/cassandra/transport/DataType.java
**Category**: Logic error in condition

---

## Root Cause

In `DataType.fromType()`, the code first unwrapped `ReversedType` to get the base type, then used `else if` to check for `DateType`. Because `DateType` was in an `else if` branch, a `ReversedType` wrapping a `DateType` (i.e., `ReversedType(DateType)`) would unwrap correctly but then skip the `DateType → TimestampType` substitution entirely, since the `else if` condition was not evaluated after the `if` branch matched. The fix changes `else if` to a standalone `if`, making both transformations apply sequentially.

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
