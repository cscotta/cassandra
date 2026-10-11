# Bug 0719: NPE in StreamSession.isKeepAliveSupported when peer version is unknown

**Commit**: 633babf0f0 · **Ticket**: n/a (follow-up to CASSANDRA-12929)
**Subsystem**: Streaming
**Category**: Missing null / bounds check

---

## Root Cause
`StreamSession.isKeepAliveSupported()` called `Gossiper.instance.getReleaseVersion(peer)` and immediately invoked `compareTo()` on the result. `getReleaseVersion` returns `null` when the peer's gossip state is not yet available (e.g. during bootstrap, or when the peer is not yet known to gossip). The null dereference produced an NPE that aborted the streaming session. The fix adds a null guard before the comparison.

### Before / After
```java
// BEFORE
private boolean isKeepAliveSupported()
{
    CassandraVersion peerVersion = Gossiper.instance.getReleaseVersion(peer);
    return peerVersion.compareTo(STREAM_KEEP_ALIVE_VERSION) >= 0;
}

// AFTER
private boolean isKeepAliveSupported()
{
    CassandraVersion peerVersion = Gossiper.instance.getReleaseVersion(peer);
    return peerVersion != null && peerVersion.compareTo(STREAM_KEEP_ALIVE_VERSION) >= 0;
}
```

---

## How to Spot
Any call to a gossip accessor that returns a version or state should be assumed capable of returning `null` for peers that are not yet fully announced. Version-comparison helpers are particularly risky because they look like simple value comparisons but silently assume non-null. Always null-check gossip-derived values before use; returning a safe default (`false` here, meaning "feature not supported") is the correct fallback.

---

## Test
No test added.
