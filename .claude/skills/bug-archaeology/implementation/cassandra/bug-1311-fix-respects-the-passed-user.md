# Bug 1311: cassandra-stress Ignores Consistency Level in User Mode

**Commit**: 4646014851 · **Ticket**: CASSANDRA-8948
**Subsystem**: tools/stress/operations/userdefined/SchemaStatement
**Category**: State not cleaned up

---

## Root Cause

`SchemaStatement`, the base class for user-defined stress operations, prepares a `BoundStatement` from a driver `PreparedStatement` but never set the consistency level on that statement. The `ConsistencyLevel` passed through the stress command line was stored in the `cl` field of the parent `Operation` class, but the constructor only built the `argumentIndex` mapping for parameters—it silently discarded `cl` without forwarding it to the driver statement via `statement.setConsistencyLevel(...)`. As a result, all user-mode operations executed at the driver's default consistency level rather than the one the user requested.

### Before / After

```java
// BEFORE – cl is received but never applied to the statement
int i = 0;
for (ColumnDefinitions.Definition definition : statement.getVariables())
    argumentIndex[i++] = spec.partitionGenerator.indexOf(definition.getName());
```

```java
// AFTER – cl is propagated to the prepared statement
int i = 0;
for (ColumnDefinitions.Definition definition : statement.getVariables())
    argumentIndex[i++] = spec.partitionGenerator.indexOf(definition.getName());

statement.setConsistencyLevel(JavaDriverClient.from(cl));
```

---

## How to Spot

When a class accepts a configuration parameter through its constructor but that parameter is not referenced anywhere in the constructor body or stored for later use, verify that it is intentionally ignored. Here, `cl` is accepted by the parent class and expected to be applied; the absence of any reference to it in the child class's setup code is the tell.

---

## Test

No test added.
