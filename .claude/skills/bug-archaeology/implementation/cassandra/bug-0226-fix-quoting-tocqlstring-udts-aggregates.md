# Bug 0226: Missing Quoting in toCqlString for UDTs and Aggregate Functions

**Commit**: 75194201f1 · **Ticket**: CASSANDRA-17918
**Subsystem**: cql3/functions, db/marshal
**Category**: Wrong serialization / deserialization

---

## Root Cause

`UserType.toCqlString()` and `UDAggregate.toCqlString()` used plain `.append()` for field names and function names respectively, which does not quote identifiers that are CQL reserved words (e.g., `token`, `desc`). This caused the generated CQL to be syntactically invalid when re-executed, making round-trip describe/restore impossible for types or aggregates with reserved-word names.

### Before / After

```java
// BEFORE (UserType)
builder.append(fieldNameAsString(i))

// BEFORE (UDAggregate)
builder.append(stateFunction().name().name)
builder.append(finalFunction().name().name)
```

```java
// AFTER (UserType)
builder.appendQuotingIfNeeded(fieldNameAsString(i))

// AFTER (UDAggregate)
builder.appendQuotingIfNeeded(stateFunction().name().name)
builder.appendQuotingIfNeeded(finalFunction().name().name)
```

---

## How to Spot

Every place that serializes a user-controlled identifier (type name, field name, function name) into CQL output must use `appendQuotingIfNeeded` or equivalent. Plain `append` is only safe for literal strings known not to conflict with reserved words; review all CQL-generation code paths for inconsistent quoting.

---

## Test

Added `testUsingReservedInCreateType` and `testDescFunctionAndAggregateShouldNotOmitQuotations` in `DescribeStatementTest`; creates a UDT and aggregate using `"token"` as a name, then verifies that `DESCRIBE` output round-trips correctly.
