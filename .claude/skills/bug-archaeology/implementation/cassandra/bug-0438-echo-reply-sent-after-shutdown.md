# Bug 0438: Echo Reply Sent After Shutdown, Causing Gossip to Re-mark Dead Node as Up

**Commit**: 12604456f9 · **Ticket**: CASSANDRA-16094
**Subsystem**: `src/java/org/apache/cassandra/service/EchoVerbHandler.java`, `service/StorageService.java`
**Category**: State not cleaned up

---

## Root Cause

When a node shuts down it sends a gossip shutdown message to peers, which mark it as dead. However, if a gossip ECHO request arrived at the shutting-down node just before its messaging service fully stopped, `EchoVerbHandler.doVerb` would respond unconditionally. The peer receiving that reply would then call `onAlive()` and re-mark the node as up, counteracting the shutdown notification that had just been processed. The verb handler had no awareness of the local shutdown state.

### Before / After

```java
// BEFORE – reply sent regardless of shutdown state
public void doVerb(MessageIn<EchoMessage> message, int id) {
    MessageOut<EchoMessage> echoMessage = new MessageOut<>(...);
    MessagingService.instance().sendReply(echoMessage, id, message.from);
}
```

```java
// AFTER – reply suppressed when node is shutting down
public void doVerb(MessageIn<EchoMessage> message, int id) {
    if (!StorageService.instance.isShutdown()) {
        MessageOut<EchoMessage> echoMessage = new MessageOut<>(...);
        MessagingService.instance().sendReply(echoMessage, id, message.from);
    }
}
```

---

## How to Spot

Any `IVerbHandler` that sends a reply without first checking node state is a candidate for this class of bug. The pattern is: a handler that answers liveness or health-probe messages (ECHO, PING) should check `isShutdown()` before responding, because even a brief overlap between shutdown-signal propagation and an in-flight probe creates a window where dead nodes appear alive. Review every handler that calls `sendReply()` for an absence of this guard.

---

## Test

`GossipShutdownTest.shutdownStayDownTest` simulates a node shutdown while gossip messages are in flight and asserts that the `onAlive()` callback is never triggered after `onDead()` has fired for that endpoint.
