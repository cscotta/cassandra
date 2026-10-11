# Bug 2053: Gossip Marks Node Alive Without Confirming Bidirectional Connectivity

**Commit**: 576efcd812 · **Ticket**: CASSANDRA-3533
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java, src/java/org/apache/cassandra/gms/EchoMessage.java, src/java/org/apache/cassandra/service/EchoVerbHandler.java, src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`Gossiper.markAlive` previously marked a node as live immediately upon receiving gossip evidence of its existence, without verifying that direct messaging to that node was actually working. When a firewall blocked direct communication (while gossip still flowed through other paths), Cassandra would mark the node `UP` and route client requests to it, causing `TimeoutException`s. The fix defers the `markAlive` transition until the node responds to a new lightweight `ECHO` message, confirming that the TCP path from the local node to the remote is open before declaring it live.

### Before / After

```java
// BEFORE: node marked live synchronously without round-trip confirmation
private void markAlive(InetAddress addr, EndpointState localState) {
    localState.markAlive();
    liveEndpoints.add(addr);
    ...
}
```

```java
// AFTER: actual markAlive logic deferred to callback after ECHO round-trip
private void markAlive(final InetAddress addr, final EndpointState localState) {
    MessageOut<EchoMessage> echoMessage = new MessageOut<>(...);
    IAsyncCallback echoHandler = new IAsyncCallback() {
        public void response(MessageIn msg) {
            localState.markAlive();
            liveEndpoints.add(addr);
            ...
        }
    };
    MessagingService.instance().sendRR(echoMessage, addr, echoHandler);
}
```

---

## How to Spot

Any place that transitions a remote node to an "available" state based solely on indirect evidence (gossip, heartbeat forwarded through a third node) rather than a direct round-trip probe is susceptible to this class of bug. Review liveness state transitions to ensure they require direct confirmation from the target node.

---

## Test

No new test added.
