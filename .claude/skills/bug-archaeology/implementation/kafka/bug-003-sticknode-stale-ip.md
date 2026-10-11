**Commit**: 123ee9e45d · **Ticket**: KAFKA-20393
**Subsystem**: clients — NetworkClient / TelemetrySender
**Category**: State not cleaned up

**Root cause**
`NetworkClient.TelemetrySender` caches a `stickyNode` to reuse the same broker for telemetry (per KIP-714). When a broker's IP address changes (e.g., pod replacement in Kubernetes), the node ID remains the same but the host/port differ. `canSendRequest()` checks only by node ID, so the stale `Node` object kept passing the check. Telemetry data was therefore sent to the old (potentially unreachable) host. The cached `Node` was never refreshed against current metadata.

**Before/After**
```java
// Before — no staleness check before reuse
if (stickyNode == null) {
    stickyNode = leastLoadedNode(now).node();
}

// After — validate stickyNode against current metadata first
if (stickyNode != null && isNodeChanged(stickyNode)) {
    log.debug("Telemetry stickyNode {} either is no longer in metadata or changed, clearing it.", stickyNode);
    stickyNode = null;
}
if (stickyNode == null) {
    stickyNode = leastLoadedNode(now).node();
}
```

**How to spot**
Any place a `Node` reference is cached across network-level reconnects without re-validating against live metadata is a candidate for this class of bug. The symptom is successful `canSendRequest()` (checks by ID only) while the actual TCP connection targets the stale IP. Particularly common in Kubernetes where pod IPs change on restart.

**Test**
`NetworkClientTest#testStickyNodeDoesNotUseStaleIpOnReconnect` — simulates broker pod replacement by switching `ManualMetadataUpdater` from `staleNode` to `freshNode` (same ID, different IP) and verifies that after the fix the reconnect attempt uses the fresh IP.
