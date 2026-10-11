**Commit**: f3da8f500e · **Ticket**: KAFKA-18936
**Subsystem**: core/src/main/scala/kafka/server/ReplicaManager.scala
**Category**: Wrong constant / default value

**Root cause**: Share-fetch requests used `FetchParams` with a `hardMaxBytesLimit` derived from the API version of the request (`versionId`). Pre-v3 fetch semantics treat the byte limit as a hard cap (returning empty data when even one record exceeds it), while v3+ semantics guarantee at least one record. `ReplicaManager.readFromLog` checked `!params.hardMaxBytesLimit` before allowing a minimum-one-message read, so when `hardMaxBytesLimit` was `true` (old-version semantics accidentally applied to share-fetch), records larger than `fetch.max.bytes` caused an infinite empty response loop. The fix removes the `versionId` parameter from `FetchParams` for share-fetch paths, always setting `minOneMessage = true` and removing the `hardMaxBytesLimit` conditional so at least one record is always returned.

**Before/After**

```scala
// Before – hardMaxBytesLimit from API version could block large records
var minOneMessage = !params.hardMaxBytesLimit
// and in remote log:
if (!remoteStorageFetchInfo.minOneMessage &&
    !remoteStorageFetchInfo.hardMaxBytesLimit &&
    firstBatchSize > maxBytes) { return EMPTY; }

// After – always allow minimum one message
var minOneMessage = true
// and in remote log:
if (!remoteStorageFetchInfo.minOneMessage && firstBatchSize > maxBytes) { return EMPTY; }
```

**How to spot**: Share-fetch (and any non-standard fetch variant) should not inherit per-version byte-limit semantics designed for the regular FETCH API. Always verify that `FetchParams` constructor arguments are appropriate for the caller's protocol, not copied from a consumer-fetch template.

**Test**: No new test in this commit; the fix is validated by the existing `DelayedShareFetchTest` and `RemoteLogManagerTest` suites, whose constructor calls are updated to remove the `versionId` argument.
