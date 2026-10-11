# Bug 2170: Stress Tool Used CQL2 Thrift API for CQL3 Queries

**Commit**: e2df26b899 · **Ticket**: CASSANDRA-4979
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/operations/
**Category**: Wrong constant / default value

---

## Root Cause

The Cassandra stress tool's CQL operation classes (`CqlInserter`, `CqlReader`, `CqlRangeSlicer`, `CqlCounterAdder`, `CqlCounterGetter`, `CqlIndexedRangeSlicer`) always called the CQL2 Thrift methods (`execute_cql_query`, `execute_prepared_cql_query`) regardless of the configured CQL version. When `--cql-version=3` was used, CQL3 queries were sent through the CQL2 wire path, which does not support CQL3 syntax and has a different consistency-level contract (CQL2 embeds `USING CONSISTENCY` inline; CQL3 passes it as a separate parameter). Additionally, the counter CF was created using `execute_cql_query` instead of `execute_cql3_query`.

### Before / After

```java
// BEFORE — always uses CQL2 Thrift call
client.execute_cql_query(ByteBuffer.wrap(formattedQuery.getBytes()), Compression.NONE);
```

```java
// AFTER — dispatches to the correct API based on version
if (session.cqlVersion.startsWith("3"))
    client.execute_cql3_query(ByteBuffer.wrap(formattedQuery.getBytes()), Compression.NONE, session.getConsistencyLevel());
else
    client.execute_cql_query(ByteBuffer.wrap(formattedQuery.getBytes()), Compression.NONE);
```

---

## How to Spot

When a tool or client is extended to support multiple protocol versions, review every Thrift (or RPC) call site to ensure it is dispatched based on the negotiated version. A version flag at the session level that is not checked at each call site is a common source of wrong-API errors.

---

## Test

No test added.
