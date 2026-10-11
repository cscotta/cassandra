# Bug 1591: Fix Native Protocol CAS Batches Returning Null

**Commit**: aa9894df43 · **Ticket**: CASSANDRA-7337
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/BatchStatement.java, src/java/org/apache/cassandra/cql3/QueryProcessor.java, src/java/org/apache/cassandra/transport/messages/BatchMessage.java
**Category**: Missing null / bounds check

---

## Root Cause

`BatchStatement.executeWithPerStatementVariables()` returned `null` for non-conditional batches instead of a `ResultMessage.Void`. When the native protocol `BatchMessage` handler called this method and forwarded the result to the client, a `null` response caused a `NullPointerException` or a malformed protocol response, breaking CAS (conditional) batches sent over the native protocol. Additionally, `BatchMessage` never computed the `hasConditions` flag, so `BatchStatement` was always constructed with `hasConditions=false`, meaning `executeWithConditions` was never dispatched even for LWT batches.

### Before / After

```java
// BEFORE — null returned for non-LWT batch path
executeWithoutConditions(getMutations(variables, local, cl, now), cl);
return null;
```

```java
// AFTER — proper void response
executeWithoutConditions(getMutations(variables, local, cl, now), cl);
return new ResultMessage.Void();
```

---

## How to Spot

Any `execute`-style method declared to return `ResultMessage` must never return `null`; callers in the transport layer forward the result directly to the client. Review all branches of such methods — especially the non-conditional fast path — to ensure a non-null `ResultMessage` is returned. Additionally, any constructor parameter that controls dispatch logic (like `hasConditions`) must be derived from the actual statement list before constructing the object.

---

## Test

No test added.
