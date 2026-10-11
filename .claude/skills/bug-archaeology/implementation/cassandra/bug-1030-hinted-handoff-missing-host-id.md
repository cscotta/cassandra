# Bug 1030: Hinted Handoff Write Throws AssertionError When Host ID Is Missing

**Commit**: e291382fd0 · **Ticket**: CASSANDRA-10485
**Subsystem**: Hinted handoff / storage proxy
**Category**: Missing null / bounds check

---

## Root Cause
`StorageProxy.writeHintForMutation()` looked up the host ID for a target endpoint and, if it was `null`, threw an `AssertionError`. This was overly aggressive: an endpoint may transiently lack a host ID when it is not a member of the token ring (e.g. a decommissioned or unknown node), which is a legitimate runtime condition rather than a programming error. The fix demotes the hard failure to a `debug` log and discards the hint gracefully. At the same time, `HintedHandOffManager.hintFor()` previously re-did the endpoint lookup from the host ID, risking a `null` endpoint and a spurious warning; the fix passes the `(InetAddress, UUID)` pair together so no secondary lookup is needed.

### Before / After
```java
// BEFORE — throws AssertionError if host ID is null
UUID hostId = StorageService.instance.getTokenMetadata().getHostId(target);
if (hostId == null) {
    logger.warn("Missing host Id for {}", target.getHostAddress());
    throw new AssertionError("Missing host Id for " + target.getHostAddress());
}
HintedHandOffManager.instance.hintFor(mutation, now, ttl, hostId).apply();

// AFTER — silently discards hint for non-ring endpoints
UUID hostId = StorageService.instance.getTokenMetadata().getHostId(target);
if (hostId != null) {
    HintedHandOffManager.instance.hintFor(mutation, now, ttl, Pair.create(target, hostId)).apply();
    StorageMetrics.totalHints.inc();
} else {
    logger.debug("Discarding hint for endpoint not part of ring: {}", target);
}

// BEFORE — hintFor() accepted only UUID and re-looked-up InetAddress
public Mutation hintFor(Mutation mutation, long now, int ttl, UUID targetId) {
    InetAddress endpoint = StorageService.instance.getTokenMetadata().getEndpointForHostId(targetId);
    if (endpoint != null) metrics.incrCreatedHints(endpoint);
    else logger.warn("Unable to find matching endpoint ...");
    ...
}

// AFTER — hintFor() accepts Pair<InetAddress, UUID>; no secondary lookup
public Mutation hintFor(Mutation mutation, long now, int ttl, Pair<InetAddress, UUID> target) {
    InetAddress endpoint = target.left;
    UUID targetId = target.right;
    metrics.incrCreatedHints(endpoint);
    ...
}
```

---

## How to Spot
Any `getHostId()` / `getEndpointForHostId()` result should be treated as nullable because token-metadata can change concurrently. An `AssertionError` on a null lookup result is a design smell in distributed code — use a graceful warning or skip logic instead. When refactoring callers to pass more context (here: the `InetAddress` alongside the UUID), verify that all downstream consumers are updated to avoid redundant, potentially-inconsistent re-lookups.

---

## Test
`HintedHandOffTest` was updated to pass `Pair.create(InetAddress.getByName("127.0.0.1"), UUID.randomUUID())` instead of a bare `UUID`, confirming the new API signature works end-to-end.
