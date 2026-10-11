# Bug 0975: isJoined returns true before node becomes a full cluster member

**Commit**: 8408c76aff · **Ticket**: CASANDRA-11007
**Subsystem**: Cluster membership / gossip
**Category**: Logic error in condition

---

## Root Cause
`StorageService.isJoined()` returned `joined && !isSurveyMode`, relying on the internal `joined` boolean flag that was set during the bootstrap/join sequence. This flag could be set to `true` before the node was actually recorded as a member in the token metadata ring, meaning `isJoined()` reported `true` during a brief window when the node had completed its local join bookkeeping but had not yet been fully recognized by the cluster. Code that gated actions on `isJoined()` (such as client-facing readiness checks) could therefore activate prematurely.

The fix replaces the flag check with an authoritative membership query: `tokenMetadata.isMember(FBUtilities.getBroadcastAddress())`, which returns `true` only once the node's address is in the live member set of the token metadata.

### Before / After
```java
// BEFORE
public boolean isJoined()
{
    return joined && !isSurveyMode;
}

// AFTER
public boolean isJoined()
{
    return tokenMetadata.isMember(FBUtilities.getBroadcastAddress());
}
```

---

## How to Spot
Local boolean flags that track multi-step cluster-join state are prone to being set at the wrong step in the sequence. Whenever a node-lifecycle guard like `isJoined()` or `isBootstrapping()` is implemented as a plain field rather than as a query against the authoritative data structure (token metadata, gossip state), check whether the field assignment happens before or after the node is externally observable as joined. Prefer querying the canonical source of truth over maintaining a separate flag.

---

## Test
No test added.
