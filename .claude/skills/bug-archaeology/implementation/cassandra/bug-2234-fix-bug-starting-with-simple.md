# Bug 2234: Cassandra Startup Fails With Simple Authentication Because Internal CQL Queries Go Through the Network Stack

**Commit**: c74a871e7a · **Ticket**: CASSANDRA-4648
**Subsystem**: src/java/org/apache/cassandra/cql3/CQLStatement.java, src/java/org/apache/cassandra/cql3/QueryProcessor.java, src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java, src/java/org/apache/cassandra/cql3/statements/SelectStatement.java (and related statement classes)
**Category**: Missing override of default method

---

## Root Cause

`QueryProcessor.processInternal()` is used during startup to query system tables (e.g., to load the authentication data). Before this fix it called the normal `execute()` path, which routes reads through `StorageProxy` and writes through the storage proxy's mutation path — both of which require a valid, fully-initialized `ClientState` including an authenticated keyspace. With `SimpleAuthenticator` enabled, the authentication bootstrap itself invokes `processInternal`, but the `ClientState` used has no keyspace set, causing a `NullPointerException` or `InvalidRequestException` before authentication can complete.

The fix adds `executeInternal(ClientState)` to the `CQLStatement` interface and implements it on every concrete statement class. `SelectStatement` reads locally via `Table.open()` / `RangeSliceVerbHandler.executeLocally()`, while `ModificationStatement` applies mutations directly with `mutation.apply()`, bypassing the network stack entirely.

### Before / After

```java
// BEFORE — processInternal went through StorageProxy
ResultMessage result = processStatement(
    getStatement(query, state).statement, state,
    Collections.<ByteBuffer>emptyList());
```

```java
// AFTER — processInternal uses the local-only path
state.setKeyspace(Table.SYSTEM_KS);
CQLStatement statement = getStatement(query, state).statement;
statement.validate(state);
ResultMessage result = statement.executeInternal(state);
```

---

## How to Spot

When an interface gains a new execution mode (e.g., "internal" vs. "external"), check whether every implementation class provides the new method. A missing `executeInternal` on any `CQLStatement` subclass would throw `UnsupportedOperationException` at runtime rather than failing at compile time, because the method was added to the interface without a default implementation. Watch for startup-path queries that share a code path with authenticated client queries.

---

## Test

No test added.
