# Bug 0469: Missing Topology Events When Multiple Nodes Share the Same Network Interface

**Commit**: 6317fefec8 · **Ticket**: CASSANDRA-15677
**Subsystem**: `src/java/org/apache/cassandra/transport/Server.java`, `src/java/org/apache/cassandra/transport/Event.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`Server.send(InetAddressAndPort, Event.NodeEvent)` suppressed topology events for any node whose broadcast native address (IP only) equalled the local node's broadcast native address. This was intended to avoid sending redundant self-events, but the comparison used only the IP address via `nodeAddress()` (returning a bare `InetAddress`), discarding the port. When multiple Cassandra nodes ran on the same host with different native ports, the IP comparison would match them all, causing legitimate decommission or status-change events from peer nodes to be silently dropped before delivery to connected drivers.

### Before / After

```java
// BEFORE
if (!endpoint.equals(FBUtilities.getBroadcastAddressAndPort()) &&
    event.nodeAddress().equals(FBUtilities.getJustBroadcastNativeAddress()))
    return;
```

```java
// AFTER
if (!endpoint.equals(FBUtilities.getBroadcastAddressAndPort()) &&
    event.nodeAddressAndPort().equals(FBUtilities.getBroadcastNativeAddressAndPort()))
    return;
```

---

## How to Spot

Anywhere that an `InetAddressAndPort` is compared for identity but only the `InetAddress` component is extracted and used, the port dimension is silently dropped. Reviewers should ensure that identity comparisons on socket addresses always include both IP and port, especially when the code is expected to work in single-host multi-node test environments or future deployments where multiple processes share an interface.

---

## Test

A new distributed test `NodeDecommissionTest.testDecomissionSucceedsForNodesOnTheSameInterface` starts a 3-node cluster using `OneNetworkInterface` provisioning strategy, decommissions node 3, and asserts that the driver's host metadata shrinks to 2.
