# Bug 2105: AssertionError When Loading Tokens for Hosts Without a Host ID

**Commit**: 828572acdb · **Ticket**: CASSANDRA-5197
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Missing null / bounds check

---

## Root Cause

During startup, `StorageService` iterates over endpoints that have saved tokens and calls `tokenMetadata.updateHostId(loadedHostIds.get(ep), ep)` unconditionally. If a node was persisted without a host ID (e.g. from an older Cassandra version or a partially-initialized node), `loadedHostIds.get(ep)` returns `null`, and `updateHostId` asserts the ID is non-null, causing an `AssertionError` that aborts startup.

The fix guards the call with `containsKey`, so `updateHostId` is only invoked when a host ID actually exists for the endpoint.

### Before / After

```java
// BEFORE
tokenMetadata.updateNormalTokens(loadedTokens.get(ep), ep);
tokenMetadata.updateHostId(loadedHostIds.get(ep), ep);
Gossiper.instance.addSavedEndpoint(ep);
```

```java
// AFTER
tokenMetadata.updateNormalTokens(loadedTokens.get(ep), ep);
if (loadedHostIds.containsKey(ep))
    tokenMetadata.updateHostId(loadedHostIds.get(ep), ep);
Gossiper.instance.addSavedEndpoint(ep);
```

---

## How to Spot

Any time a value is retrieved from a map that was populated from a separate persistence source (e.g. system tables with optional columns), use `containsKey` before passing the result to methods that assert non-null. Avoid assuming two parallel maps are always in sync.

---

## Test

No test added.
