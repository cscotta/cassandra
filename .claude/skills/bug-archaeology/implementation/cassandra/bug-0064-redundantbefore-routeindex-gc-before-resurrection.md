# Bug 0064: RedundantBefore Used Without Persistence Guard / RouteIndex Indexed Full Route Instead of Touches

**Commit**: 5086846494 · **Ticket**: CASSANDRA-20361
**Subsystem**: `db/compaction/CompactionIterator`, `index/accord/RouteIndexFormat`, `service/accord`
**Category**: State not cleaned up

---

## Root Cause

Two distinct correctness defects are bundled in this commit.

First, `CompactionIterator` retrieved a `RedundantBefore.Entry` (the live, in-memory view) instead of the persisted `RedundantBefore.Bounds` snapshot. Using the live entry meant compaction could act on a more recent `RedundantBefore` state than what was flushed to the journal, so after a crash the compacted data would reference transaction IDs that the recovered state no longer knew were redundant — potentially resurrecting old transaction state.

Second, `RouteIndexFormat` serialized the full `Route<?>` (which may include the home shard and extra replicas) into the route index rather than serializing only `Participants.touches()`. As a result the index contained routing keys that were not actually touched by the transaction, leading to spurious matches on range scans and incorrect query results.

### Before / After

```java
// BEFORE – CompactionIterator
RedundantBefore.Entry redundantBeforeEntry = redundantBefore.get(tokenKey.toUnseekable());
```

```java
// AFTER – CompactionIterator
RedundantBefore.Bounds redundantBeforeEntry = redundantBefore.get(tokenKey.toUnseekable());
```

```java
// BEFORE – RouteIndexFormat
public static ByteBuffer serialize(Route<?> value) throws IOException { ... }
Route<?> route = participants.route();
return serialize(participants.route());
```

```java
// AFTER – RouteIndexFormat
public static ByteBuffer serialize(Participants<?> value) throws IOException { ... }
Participants<?> touches = participants.touches();
return serialize(touches);
```

---

## How to Spot

Whenever code that drives compaction or garbage-collection decisions reads Accord state, verify that it reads only the *persisted* (journal-synced) snapshot rather than the latest in-memory object. Similarly, when indexing routing information for range queries, confirm that the stored set is scoped to the actual participants (`touches`) rather than the broader `Route` object, which may include non-participant replicas.

---

## Test

`RouteIndexTest` extended with proper `RedundantBefore` initialization and non-mocked compaction info to exercise the correct `Bounds`-based path; no direct regression test for the `GC_BEFORE` resurrection scenario was added in this commit.
