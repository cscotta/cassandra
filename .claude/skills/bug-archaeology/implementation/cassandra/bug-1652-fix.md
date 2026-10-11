# Bug 1652: Fix StorageProxy#syncWriteToBatchlog()

**Commit**: 02ffaff69d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Wrong constant / default value

---

## Root Cause

`StorageProxy.syncWriteToBatchlog()` called `MessagingService.sendRR(message, target, handler)` — a three-argument overload. Because `AbstractWriteResponseHandler` implements `IAsyncCallback`, Java resolved the call to the generic `sendRR(MessageOut, InetAddress, IAsyncCallback)` overload rather than the mutation-specific `sendRR(MessageOut<? extends IMutation>, InetAddress, AbstractWriteResponseHandler, boolean allowHints)` overload. The generic overload does not pass `ConsistencyLevel` and `allowHints` to `addCallback`, meaning hint-tracking was applied incorrectly for batchlog writes. The fix explicitly passes `false` for `allowHints`, selecting the correct mutation overload and ensuring no hints are triggered for batchlog mutations.

### Before / After

```java
// BEFORE
MessagingService.instance().sendRR(message, target, handler);
// ...
MessagingService.instance().sendRR(
    BatchlogManager.getBatchlogMutationFor(mutations, uuid, targetVersion).createMessage(),
    target, handler);
```

```java
// AFTER
MessagingService.instance().sendRR(message, target, handler, false);
// ...
MessagingService.instance().sendRR(
    BatchlogManager.getBatchlogMutationFor(mutations, uuid, targetVersion).createMessage(),
    target, handler, false);
```

---

## How to Spot

When a class hierarchy causes overload resolution ambiguity (a subtype implementing multiple interfaces), an explicit method selection via an extra discriminating argument or cast is needed. Review every call-site where a handler object satisfies multiple overload signatures and confirm the intended overload is actually selected.

---

## Test

No test added.
