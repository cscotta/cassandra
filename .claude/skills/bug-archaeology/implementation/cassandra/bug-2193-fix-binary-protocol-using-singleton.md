# Bug 2193: Binary Protocol Corrupts Stream IDs by Reusing Singleton Response

**Commit**: 3b0aa84ca3 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/transport/messages/ResultMessage.java, src/java/org/apache/cassandra/transport/Message.java, src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ResultMessage.Void` was implemented as a singleton (via a `Holder` inner class). Every response to a statement that produced no result set returned the same shared instance. Because each `Message` carries mutable `streamId` and `connection` fields that are set per-request, concurrent requests that both returned a `Void` result would race to write their stream ID into the singleton, causing one or both responses to be sent with the wrong stream ID. The fix removes the singleton entirely and allocates a fresh `Void` instance for each response.

### Before / After

```java
// BEFORE – singleton reused across concurrent requests
return result == null ? ResultMessage.Void.instance() : result;
```

```java
// AFTER – new instance per response
return result == null ? new ResultMessage.Void() : result;
```

---

## How to Spot

Any protocol message class that carries per-request mutable fields (stream ID, connection reference) and is also exposed as a singleton is a data race waiting to happen. During code review, check that "empty" or "void" response objects are never shared across requests; message objects must be treated as value objects that are created once and discarded.

---

## Test

No test added.
