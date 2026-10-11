# Bug 008: Ensure that gossip state for LEFT nodes is expired eventually

**Commit**: ad3f0e8f41 · **Ticket**: CASSANDRA-21035
**Subsystem**: Gossip / Cluster Membership
**Category**: State not cleaned up

---

## Root Cause

When a node decommissions, is removed, or is assassinated, the remaining cluster members each calculate an independent expiry deadline for the LEFT node's gossip state, meaning the state may never be fully purged from gossip across the cluster (each node's deadline diverges). The gossip quarantine mechanism, which was originally designed to prevent stale-state resurrection, suppressed further propagation of the LEFT status after the node was removed, making it impossible for all peers to agree on a common expiry timestamp. Because each peer derives its own expiry time locally, LEFT node state could linger indefinitely and the gossip state map would never converge.

The fix introduces a `gossip_quarantine_disabled` config option (and a JMX hotprop) that, when enabled, causes the coordinating node (the decommissioning node itself, or the removenode/assassinate coordinator) to broadcast a canonical LEFT status—including a shared expiry timestamp—to all remaining cluster members. Peers that have already seen a LEFT status with a non-zero version retain it rather than overwriting it with a locally-derived value, allowing the expiry time to converge.

### Before / After

```java
// BEFORE — expiry time calculated independently per peer; quarantine suppresses propagation
long expireTime = getExpireTimeForEndpoint(endpoint);
if (!epState.isAlive() && (now > expireTime)
    && (!metadata.directory.allAddresses().contains(endpoint)))
{
    runInGossipStageBlocking(() -> evictFromMembership(endpoint));
}

// BEFORE — addExpireTimeForEndpoint always stored the value, even 0
public void addExpireTimeForEndpoint(InetAddressAndPort endpoint, long expireTime)
{
    logger.debug("adding expire time for endpoint : {} ({})", endpoint, expireTime);
    expireTimeEndpointMap.put(endpoint, expireTime);
}

// AFTER — expiry check extracted; guard against 0 timestamps; quarantine can be bypassed
void evictIfExpired(InetAddressAndPort endpoint, EndpointState epState, Directory directory, long now)
{
    if (!epState.isAlive() && (!directory.allJoinedEndpoints().contains(endpoint)))
    {
        long expireTime = getExpireTimeForEndpoint(endpoint);
        if (now > expireTime)
        {
            logger.info("Reached gossip expiry time for endpoint : {} ({})", endpoint, expireTime);
            runInGossipStageBlocking(() -> evictFromMembership(endpoint));
        }
    }
}

// AFTER — skip recording a zero expiry time
if (expireTime == 0L)
    logger.debug("Supplied expire time for {} was 0, not recording", endpoint);
else
    expireTimeEndpointMap.put(endpoint, expireTime);
```

---

## How to Spot

When a subsystem combines a "suppress updates after terminal state" mechanism (here: quarantine) with per-node calculation of time-based cleanup deadlines, there is a structural risk that the deadline never converges and cleanup never happens on all nodes. Reviewers should ask: "If each node calculates this deadline independently and propagation of the terminal state is suppressed, can the deadline ever agree cluster-wide?" If the answer is no, either the deadline must be piggybacked in the terminal state message itself (as done here via the expiry time encoded in `STATUS_WITH_PORT`) or the suppression mechanism must be weakened enough to let the authoritative value propagate.

---

## Test

`GossipExpiryTestBase.testExpiryOfLeftStateWithoutQuarantine` / `testExpiryOfLeftStateWithQuarantine` verify (for decommission, removenode, and assassinate scenarios) that after a node leaves, all remaining peers eventually purge its gossip state within a bounded time, and when quarantine is disabled, that the `STATUS_WITH_PORT` expiry timestamp converges to a single value across the cluster.
