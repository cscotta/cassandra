# Bug 0414: Schema Mutations for System Keyspaces Pushed to Peers During Local Setup

**Commit**: 1f686fd634 · **Ticket**: CASSANDRA-16387
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java, src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: State not cleaned up

---

## Root Cause

When a node initialized or evolved the schema for distributed system keyspaces (trace, auth), it called `MigrationManager.announceGlobally`, which applied the schema mutation locally and then pushed it to all live gossip peers. For internal system keyspaces this push is unnecessary—each node manages its own system schema evolution independently—and in early startup (before the messaging layer is fully ready) or in test clusters with internode encryption requirements it could cause schema-agreement timeouts or unexpected schema propagation. The fix introduces `announceWithoutPush`, which applies the mutation locally only, and wires the system-keyspace evolution calls to use it instead.

### Before / After

```java
// BEFORE — evolving the trace keyspace pushed the mutation to all peers
evolveSystemKeyspace(TraceKeyspace.metadata(), TraceKeyspace.GENERATION)
    .ifPresent(MigrationManager::announceGlobally);
```

```java
// AFTER — local application only
Optional<Mutation> mutation = evolveSystemKeyspace(TraceKeyspace.metadata(), TraceKeyspace.GENERATION);
mutation.ifPresent(value ->
    FBUtilities.waitOnFuture(MigrationManager.announceWithoutPush(Collections.singleton(value))));
```

---

## How to Spot

Schema changes for internal system keyspaces should never be broadcast to peers because every node applies them independently during startup. Whenever `announce(…, false)` or an equivalent global broadcast is called from system-keyspace setup code, confirm that the broadcast is truly required or whether a local-only apply suffices.

---

## Test

`InternodeEncryptionEnforcementTest` was extended to verify that schema changes succeed or fail predictably based on internode TLS configuration, which was previously interfered with by spurious system-keyspace schema pushes.
