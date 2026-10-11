# Bug 0454: Gossip Stopped Before Client Transports During Shutdown

**Commit**: acbaeb1ee8 · **Ticket**: CASSANDRA-15816
**Subsystem**: `src/java/org/apache/cassandra/service/StorageService.java`
**Category**: Logic error in condition

---

## Root Cause

`StorageService.stopTransports()` stopped gossip first, then shut down the RPC and native transports. Gossip is the cluster's liveness signaling channel: stopping it before client transports means the node stops advertising itself as alive to peers before it has finished draining in-flight client requests. Peers may then route new requests to this node (since they saw it alive recently), only to have those requests fail because native transport shuts down moments later. The correct order is: stop client-facing transports first, then stop gossip last so peers get an accurate picture of the node's state.

### Before / After

```java
// BEFORE
public void stopTransports() {
    if (isInitialized()) {
        logger.error("Stopping gossiper");
        stopGossiping();             // gossip stopped first
    }
    if (isRPCServerRunning()) stopRPCServer();
    if (isNativeTransportRunning()) stopNativeTransport();
}
```

```java
// AFTER
public void stopTransports() {
    if (isRPCServerRunning()) stopRPCServer();
    if (isNativeTransportRunning()) stopNativeTransport();
    if (isInitialized()) {           // gossip stopped last
        logger.error("Stopping gossiper");
        stopGossiping();
    }
}
```

---

## How to Spot

Shutdown sequences in distributed systems must follow an "outside-in" ordering: stop advertising availability (gossip / service discovery) last, not first. Any `stopTransports`-style method that silences gossip or deregisters from service discovery before closing client-facing sockets inverts the safe order and creates a window where peers think the node is down while clients are still being served (or vice versa).

---

## Test

No test added.
