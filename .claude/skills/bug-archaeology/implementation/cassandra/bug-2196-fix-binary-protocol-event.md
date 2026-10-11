# Bug 2196: Binary Protocol NEW_NODE Event Sent Before Native Transport Is Ready

**Commit**: 63a8bfe030 · **Ticket**: CASSANDRA-4679
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java, src/java/org/apache/cassandra/transport/Server.java, src/java/org/apache/cassandra/service/CassandraDaemon.java
**Category**: State not cleaned up

---

## Root Cause

The native transport `EventNotifier` subscribed directly to low-level Gossip events (`IEndpointStateChangeSubscriber`). The `onJoin` gossip callback fires as soon as a node is seen in gossip, which is before that node's binary protocol server is actually accepting connections. This meant clients receiving a `NEW_NODE` topology event would immediately try to connect to the new node and be refused. The fix introduces a higher-level `IEndpointLifecycleSubscriber` interface that `StorageService` populates after completing ring-join bookkeeping. The daemon startup sequence was also reordered so that `maybeJoinRing()` is called only after the native transport server is already listening, ensuring that by the time peers receive the gossip `NEW_NODE` event the local native server is ready.

### Before / After

```java
// BEFORE – EventNotifier subscribed to raw Gossip; fires too early
Gossiper.instance.register(notifier);
// EventNotifier.onJoin() → TopologyChange.newNode() fired before transport is up
```

```java
// AFTER – EventNotifier implements IEndpointLifecycleSubscriber (StorageService-level)
StorageService.instance.register(notifier);
// onJoinCluster() called only from StorageService after state is fully reconciled
// CassandraDaemon starts native transport BEFORE calling maybeJoinRing()
```

---

## How to Spot

When a component needs to notify external clients of "node available" events, verify that the notification fires only after all server ports are listening. Subscribing to a gossip-layer event is almost always too early because gossip propagates independently of transport readiness. A dedicated higher-level lifecycle interface whose methods are invoked by `StorageService` at well-defined points in the startup sequence is the correct hook.

---

## Test

No test added.
