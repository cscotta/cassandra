# Bug 0110: NoSuchElementException When Keyspace Dropped During Paxos Cleanup

**Commit**: a80663de61 · **Ticket**: CASSANDRA-20320
**Subsystem**: service/paxos/cleanup
**Category**: Missing null / bounds check

---

## Root Cause

`PaxosCleanupLocalCoordinator.maybeStartRepair` called `PaxosRepair.validatePeerCompatibility`, which internally called `Participants.get(metadata, table, ...)`. If the keyspace for the given `TableMetadata` had been concurrently dropped, `DistributedSchema.getKeyspaceMetadata` threw a `NoSuchElementException` because it unconditionally retrieved the keyspace without checking for its existence. The fix adds an explicit null check for the keyspace in `PaxosCleanupLocalCoordinator` before entering peer-compatibility validation: if the keyspace is unknown, the repair is failed gracefully with an informative message rather than crashing with an uncaught exception. Additionally, `validatePeerCompatibility` now accepts a `ClusterMetadata` parameter instead of fetching it internally, eliminating a second TOCTOU window where the metadata could change between the null check and the validation call.

### Before / After

```java
// BEFORE — no keyspace existence check; crashes if keyspace is dropped concurrently
if (!PaxosRepair.validatePeerCompatibility(ctx, table, ranges))
{
    fail("Unsupported peer versions ...");
    return;
}
```

```java
// AFTER — explicit null check before entering validation
ClusterMetadata metadata = ClusterMetadata.current();
if (metadata.schema.getKeyspace(table.keyspace) == null)
{
    fail("Unknown keyspace: " + table.keyspace);
    return;
}
if (!PaxosRepair.validatePeerCompatibility(ctx, metadata, table, ranges))
{
    fail("Unsupported peer versions ...");
    return;
}
```

---

## How to Spot

Any code that retrieves schema objects (keyspaces, tables) that could be concurrently dropped must guard the retrieval with a null check and handle the absent case gracefully. This is especially important in background repair and cleanup processes that run asynchronously relative to DDL operations. Look for calls to `getKeyspaceMetadata` / `getTableMetadata` that do not check for null returns, particularly inside async tasks or futures.

---

## Test

`PaxosRepair2Test.legacyPurgeRepairLoop` was extended: a background thread drops the keyspace while a `PaxosCleanupLocalCoordinator` is in flight; the test asserts the coordinator fails with the message "Unknown keyspace" instead of throwing `NoSuchElementException`.
