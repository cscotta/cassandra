**Commit**: 62dc982ce9 · **Ticket**: KAFKA-17396
**Subsystem**: core/ShareFetchContext / ShareSessionCache
**Category**: State not cleaned up

**Root cause**
When a Share Fetch request arrived with the final epoch, `ShareFetchContext.newContext()` and `acknowledgeSession()` removed the session from the `ShareSessionCache` before `releaseSession()` had been called. This meant the partitions associated with the session were never explicitly released back to the broker's tracking structures, creating a resource leak where partition state was left dangling after a session ended. The fix defers session removal from the cache until `releaseSession()` is explicitly called, ensuring partitions are always released through the proper teardown path.

**Before/After**
```java
// Before: session removed at newContext() for final epoch
if (isFinalEpoch(requestEpoch)) {
    cache.remove(shareSessionKey);  // partitions never released
}

// After: removal deferred to releaseSession()
// newContext() / acknowledgeSession() no longer remove session;
// caller must invoke releaseSession() which then removes from cache
void releaseSession() {
    partitions.forEach(this::releasePartition);
    cache.remove(shareSessionKey);
}
```

**How to spot**
In session-based protocols, resource release must happen in a single terminal method, not split across the request-routing and session-cleanup paths. If `remove()` is called before `release()`, any cleanup that depends on session state will operate on already-removed data and silently skip the cleanup.

**Test**
`ShareFetchContextTest` updated to verify that partitions are released before the session is evicted from the cache.
