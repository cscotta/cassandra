# Bug 0288: BulkLoader Triggers Unintended Schema Init via SystemKeyspace.getPreferredIP()

**Commit**: 5e39c54678 · **Ticket**: CASSANDRA-17740
**Subsystem**: tools / streaming
**Category**: State not cleaned up

---

## Root Cause

`StreamingMultiplexedChannel` unconditionally called `SystemKeyspace.getPreferredIP()` when opening a file-transfer connection, even when the streaming subsystem was being used by the `BulkLoader` tool rather than a running daemon. `SystemKeyspace.getPreferredIP()` queries the local `system.peers_v2` table, which requires schema to be initialized; attempting this from the bulk loader context (which has no access to the local system tables and only uses client-side metadata) caused unintended schema initialization and incorrect behavior.

### Before / After

```java
// BEFORE (StreamingMultiplexedChannel)
InetAddressAndPort connectTo = SystemKeyspace.getPreferredIP(to);
return fileTransferExecutor.submit(new FileStreamTask((OutgoingStreamMessage) message, connectTo));
```

```java
// AFTER
InetAddressAndPort connectTo = factory.supportsPreferredIp() ? SystemKeyspace.getPreferredIP(to) : to;
return fileTransferExecutor.submit(new FileStreamTask((OutgoingStreamMessage) message, connectTo));
```

---

## How to Spot

When a shared subsystem (streaming, messaging) exposes operations that require local system state (system table lookups, daemon initialization), any code path that funnels tool contexts through the same call site becomes a latent trigger for unintended initialization. Review every call to `SystemKeyspace.*` inside shared streaming/connection code and ask whether it will ever be reached from a tool that has not run `DatabaseDescriptor.init()` or `MigrationManager` setup. A `Preconditions.checkState(DatabaseDescriptor.isDaemonInitialized())` guard added at the entry point of `getPreferredIP()` makes the constraint explicit and visible during code review.

---

## Test

No targeted unit test added; the fix is validated by the existing bulk-loader integration tests that exercise `BulkLoadConnectionFactory`.
