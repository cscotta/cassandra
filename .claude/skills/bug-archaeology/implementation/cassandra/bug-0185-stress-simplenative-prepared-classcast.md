# Bug 0185: cassandra-stress simplenative Mode ClassCastException With Prepared Statements

**Commit**: 0f2af6ab3f · **Ticket**: CASSANDRA-18744
**Subsystem**: tools/stress
**Category**: Wrong serialization / deserialization

---

## Root Cause

`SimpleClientWrapper.createPreparedStatement` returned `client.prepare(cqlQuery).statementId.bytes` — a raw `byte[]` — instead of the `ResultMessage.Prepared` object itself. Later, `SimpleClientWrapper.execute(Object preparedStatement, ...)` cast the stored object back to `ResultMessage.Prepared`, causing a `ClassCastException` at runtime when stress ran in `simplenative` mode with prepared statements enabled. The `JavaDriverWrapper` stored the correct `PreparedStatement` object, so the native (Java Driver) path worked fine; only the `SimpleClient` path was broken.

### Before / After

```java
// BEFORE
public Object createPreparedStatement(String cqlQuery)
{
    return client.prepare(cqlQuery).statementId.bytes;  // returns byte[], not Prepared
}

public <V> V execute(Object preparedStatement, ByteBuffer key, List<Object> queryParams, ResultHandler<V> handler)
{
    return handler.simpleClientHandler().apply(
            client.executePrepared(
                    (ResultMessage.Prepared) preparedStatement,  // ClassCastException here
                    ...));
}
```

```java
// AFTER
public ResultMessage.Prepared createPreparedStatement(String cqlQuery)
{
    return client.prepare(cqlQuery);  // return full Prepared object
}
```

---

## How to Spot

When a `ClientWrapper`-style interface declares `createPreparedStatement` returning `Object` and a paired `execute` method casts that object to a specific type, trace each implementation to confirm the returned type is actually castable. The use of raw `Object` as a prepared-statement handle masks type mismatches that the compiler would otherwise catch; prefer a generic interface parameterized on the prepared-statement type.

---

## Test

Invokes cassandra-stress with `simplenative prepared cql3`, `simplenative cql3`, `native cql3`, and `native unprepared cql3` modes and asserts a clean exit for each.
