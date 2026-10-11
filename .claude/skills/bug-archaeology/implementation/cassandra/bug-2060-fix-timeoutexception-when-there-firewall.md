# Bug 2060: Gossip Marks Node Alive Before Confirming Reachability

**Commit**: a7b2ff65ac · **Ticket**: CASSANDRA-3533
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java, src/java/org/apache/cassandra/gms/EchoMessage.java, src/java/org/apache/cassandra/service/EchoVerbHandler.java, src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Logic error in condition

---

## Root Cause

When gossip decided a node should transition from UNREACHABLE to ALIVE, `markAlive()` immediately updated the live-endpoints set and notified all subscribers — without verifying that the node was actually reachable at the network layer. Behind a one-way firewall the local node could receive gossip from a remote peer (showing it is alive) yet be unable to open a return connection, leading to `TimeoutException` when Cassandra tried to coordinate requests to that peer. The fix introduces an `ECHO` round-trip: `markAlive()` now sends an `EchoMessage` to the candidate node and only promotes it to the live set inside the async response callback, confirming bidirectional connectivity before the rest of the cluster is notified.

### Before / After

```java
// BEFORE
private void markAlive(InetAddress addr, EndpointState localState) {
    localState.markAlive();
    liveEndpoints.add(addr);
    // ... notify subscribers immediately
}
```

```java
// AFTER
private void markAlive(final InetAddress addr, final EndpointState localState) {
    MessageOut<EchoMessage> echoMessage = new MessageOut<>(MessagingService.Verb.ECHO, ...);
    IAsyncCallback echoHandler = new IAsyncCallback() {
        public void response(MessageIn msg) {
            localState.markAlive();
            liveEndpoints.add(addr);
            // ... notify subscribers only after echo reply received
        }
    };
    MessagingService.instance().sendRR(echoMessage, addr, echoHandler);
}
```

---

## How to Spot

Any transition to "healthy/reachable" that is inferred from received traffic (one direction) without an active probe risks false-positive liveness in asymmetric network scenarios. Look for liveness state changes triggered by passive receipt of messages rather than confirmed two-way communication.

---

## Test

No test added.
