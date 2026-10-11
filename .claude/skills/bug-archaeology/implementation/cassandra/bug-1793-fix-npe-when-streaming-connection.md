# Bug 1793: Fix NPE When Streaming Connection Is Not Yet Ready

**Commit**: 41ffca1281 · **Ticket**: CASSANDRA-6210
**Subsystem**: src/java/org/apache/cassandra/streaming/ConnectionHandler.java
**Category**: Missing null / bounds check

---

## Root Cause

`IncomingMessageHandler` and `OutgoingMessageHandler` were constructed with a `Socket` argument inside `initiate()` / `initiateOnReceivingSide()`, but if a message was enqueued on the outgoing handler before the socket was assigned (i.e., before `initiateOnReceivingSide` completed), the handler's `socket` field was `null`, causing an NPE when `getWriteChannel()` or `getReadChannel()` tried to use it. The fix pre-constructs both handlers in `ConnectionHandler`'s constructor with no socket, then assigns the socket lazily inside `start(socket, version)`, ensuring the handlers exist immediately but the socket is only accessed once streaming has actually begun.

### Before / After

```java
// BEFORE – constructed only when the socket is available; NPE if used before that
outgoing = new OutgoingMessageHandler(session, socket, version);
outgoing.start();
```

```java
// AFTER – handler exists from construction; socket set atomically when start() is called
// In ConnectionHandler constructor:
this.outgoing = new OutgoingMessageHandler(session);
// Later, in initiate()/initiateOnReceivingSide():
outgoing.start(socket, version);
```

---

## How to Spot

When a class field is assigned inside one method but read in a concurrent thread that may run before that assignment, the window between object creation and field assignment is a null-access hazard. Prefer constructing objects in a fully initialised state or ensure the first access is gated on an explicit ready signal.

---

## Test

No test added.
