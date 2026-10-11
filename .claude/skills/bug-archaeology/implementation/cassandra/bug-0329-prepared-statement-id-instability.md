# Bug 0329: Prepared Statement ID Instability After CASSANDRA-15252

**Commit**: 242f7f9b18 · **Ticket**: CASSANDRA-17248
**Subsystem**: src/java/org/apache/cassandra/cql3
**Category**: State not cleaned up

---

## Root Cause

CASSANDRA-15252 changed how prepared statement IDs (MD5 digests) are computed: fully-qualified statements now hash without the keyspace, while unqualified statements hash with it. During a rolling upgrade the new node must still return the old-style ID (hash-with-keyspace) so that statements prepared on old nodes remain executable, and then switch to the new-style ID once every node in the cluster is upgraded. The original fix attempted this by checking `Gossiper.getMinVersion` with a 20 ms timeout, but the logic was incomplete: the `storePreparedStatement` path stored both variants correctly, yet the cache lookup path returned the wrong variant depending on cluster state, and both cache entries were not required to exist before serving a cached response. This caused clients to receive an unstable ID (the same logical statement would return a different ID on re-prepare) and could trigger an infinite reprepare loop when a new node received an EXECUTE with an old-style ID that was not in its cache.

### Before / After

```java
// BEFORE – stored both variants but only checked minVersion with 20 ms timeout
ResultMessage.Prepared newBehavior = storePreparedStatement(queryString, null, prepared, forThrift);
ResultMessage.Prepared oldBehavior = clientState.getRawKeyspace() != null
    ? storePreparedStatement(queryString, clientState.getRawKeyspace(), prepared, forThrift)
    : newBehavior;
CassandraVersion minVersion = Gossiper.instance.getMinVersion(20, TimeUnit.MILLISECONDS);
// ... returned old or new behaviour but didn't guarantee both were cached
```

```java
// AFTER – safeToReturnCached only when BOTH hashes are present in cache
boolean safeToReturnCached = cachedWithoutKeyspace != null && cachedWithKeyspace != null;
if (safeToReturnCached) {
    if (newPreparedStatementBehaviour) {
        if (cachedWithoutKeyspace.fullyQualified)
            return new ResultMessage.Prepared(hashWithoutKeyspace, cachedWithoutKeyspace);
        if (clientState.getRawKeyspace() != null && !cachedWithKeyspace.fullyQualified)
            return new ResultMessage.Prepared(hashWithKeyspace, cachedWithKeyspace);
    } else {
        return new ResultMessage.Prepared(hashWithKeyspace, cachedWithKeyspace);
    }
} else {
    evictPrepared(hashWithKeyspace);
    evictPrepared(hashWithoutKeyspace);
}
```

---

## How to Spot

When a cache serves as the authoritative source for a value that has multiple representations (e.g., with-keyspace vs. without-keyspace hashes), a cache hit on only one representation should be treated the same as a miss; returning a partially-populated result violates the invariant that the response must be deterministic. Any "version gate" that only checks gossip with a short timeout is also a red flag: the gossip snapshot can be stale, so checks must be retried and the result must be sticky once the threshold is crossed.

---

## Test

`ReprepareFuzzTest` (distributed) randomises keyspace/USE combinations across prepare and execute calls and asserts that the returned statement ID is stable across re-prepares on all nodes in a mixed-version cluster.
