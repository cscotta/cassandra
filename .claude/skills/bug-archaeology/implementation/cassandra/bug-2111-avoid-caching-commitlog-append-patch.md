# Bug 2111: RowMutation Pre-serialized Buffer Cache Populated but Never Useful on Append Path

**Commit**: 1c79426ab8 · **Ticket**: CASSANDRA-5199
**Subsystem**: src/java/org/apache/cassandra/db/RowMutation.java, src/java/org/apache/cassandra/db/commitlog/CommitLogSegment.java
**Category**: State not cleaned up

---

## Root Cause

`RowMutation` maintained a `preserializedBuffers` map keyed by messaging version to cache serialized byte arrays. The commit log write path called `getSerializedBuffer()` which populated this cache, but the cached bytes were never reused by any other caller on the append path — making every cache entry a no-op allocation. Additionally, `fromBytes()` attempted to pre-populate the cache to avoid re-serialization, but this optimization was also unused. The entire caching mechanism was dead weight that held serialized mutations in memory longer than necessary and complicated the code. The fix removes `preserializedBuffers`, `getSerializedBuffer`, and the cache-populating logic in `fromBytes`, replacing the single call site in the hints path with a direct `FBUtilities.serialize` call.

### Before / After

```java
// BEFORE: CommitLogSegment.write()
byte[] serializedRow = rowMutation.getSerializedBuffer(MessagingService.current_version);
// getSerializedBuffer cached in preserializedBuffers map, never reused
```

```java
// AFTER: CommitLogSegment.write()
byte[] serializedRow = FBUtilities.serialize(rowMutation, RowMutation.serializer, MessagingService.current_version);
// no caching; caller is responsible for lifecycle
```

---

## How to Spot

Caching infrastructure that is only ever written to but never read from (aside from the single writer) is dead code. Review cache hit/miss paths: if every code path calls `put` before `get`, the cache provides no benefit and only adds memory pressure. Audit all consumers of a cache before adding it.

---

## Test

No test added.
