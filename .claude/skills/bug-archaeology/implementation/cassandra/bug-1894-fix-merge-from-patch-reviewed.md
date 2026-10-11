# Bug 1894: Fix merge — MessagingService sendRR missing consistency-level overload

**Commit**: 53f7c328ae · **Ticket**: CASSANDRA-6154
**Subsystem**: src/java/org/apache/cassandra/net, src/java/org/apache/cassandra/service
**Category**: Logic error in condition

---

## Root Cause

After merging branch 6132, `MessagingService.sendRR()` lacked an overload that accepted a `ConsistencyLevel` parameter. Call sites in `StorageProxy` that needed to register a write callback with consistency-level information were directly calling `addCallback()` followed by `sendOneWay()` as two separate steps. This bypassed the normal single-method path and, more critically, counter mutation sends and forwarded writes could fail to properly associate the consistency level with the in-flight callback. The fix adds a `sendRR(message, to, cb, timeout, consistencyLevel)` overload that delegates to the appropriate `addCallback` variant in one atomic call.

### Before / After

```java
// BEFORE — two-step pattern; consistency level passed separately
int id = MessagingService.instance().addCallback(handler, message, target, message.getTimeout(), handler.consistencyLevel);
MessagingService.instance().sendOneWay(message, id, target);
```

```java
// AFTER — unified call
int id = MessagingService.instance().sendRR(message, target, handler, message.getTimeout(), handler.consistencyLevel);
```

---

## How to Spot

Call sites that split a conceptually atomic "register callback + send" operation into two separate method calls are prone to being left out of sync when the API evolves. Look for paired `addCallback` / `sendOneWay` calls that could be collapsed into a single `sendRR`.

---

## Test

No test added.
