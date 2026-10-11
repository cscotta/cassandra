# Bug 1897: addCallback Missing ConsistencyLevel for XDC Forwarded Requests

**Commit**: 6025ba85b3 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Wrong constant / default value

---

## Root Cause

In `StorageProxy`, when a write is forwarded to replicas for cross-datacenter (XDC) replication, `MessagingService.addCallback` was called without passing the `ConsistencyLevel`. The non-XDC path had just been updated to include the consistency level, but the XDC forwarding loop was missed. Without the consistency level, the callback registration used a different overload that could not perform proper consistency tracking for the forwarded messages.

### Before / After

```java
// BEFORE
String id = MessagingService.instance().addCallback(
    handler, message, destination, message.getTimeout());
```

```java
// AFTER
String id = MessagingService.instance().addCallback(
    handler, message, destination, message.getTimeout(), handler.consistencyLevel);
```

---

## How to Spot

When a method signature is updated to require an additional parameter, grep for all call sites — especially ones inside conditional branches or separate loops — to ensure all callers are updated in lockstep.

---

## Test

No test added.
