# Bug 1009: MV race condition during node decommission

**Commit**: c184e8c14b · **Ticket**: CASSANDRA-10674
**Subsystem**: Materialized Views / StorageProxy
**Category**: Race condition / missing synchronization

---

## Root Cause
`ViewUtils.getViewNaturalEndpoint()` was designed to return the single replica that should receive a materialized-view mutation.  When called during a node decommission the local node is in the process of handing off ranges; it may still be processing base-table writes for tokens it is no longer authoritative for.  The old code threw a `RuntimeException("Trying to get the view natural endpoint on a non-data replica")` in that case, with a narrow workaround that re-used a wrong early-return for the pending-endpoints case.  `StorageProxy` called this method and propagated the exception, crashing the write.  The fix changes the return type to `Optional<InetAddress>` — returning `Optional.empty()` when the local node is not a base replica — and `StorageProxy` stores unpaired mutations in the local batchlog for replay rather than failing hard.

### Before / After
```java
// BEFORE
public static InetAddress getViewNaturalEndpoint(...)
{
    ...
    if (baseIdx < 0)
    {
        if (StorageService.instance.getTokenMetadata()
                          .pendingEndpointsFor(viewToken, keyspaceName).size() > 0)
            return FBUtilities.getBroadcastAddress(); // wrong: uses self regardless
        throw new RuntimeException("Trying to get the view natural endpoint on a non-data replica");
    }
    return viewEndpoints.get(baseIdx);
}

// AFTER
public static Optional<InetAddress> getViewNaturalEndpoint(...)
{
    ...
    if (baseIdx < 0)
        return Optional.empty();   // caller will batch-log and replay later
    return Optional.of(viewEndpoints.get(baseIdx));
}
```

---

## How to Spot
Any method that returns a mandatory value but silently falls back to an incorrect sentinel (here `FBUtilities.getBroadcastAddress()`) when the precondition is not met is a candidate for this pattern.  Prefer returning `Optional` or a nullable result and let callers decide how to handle absence.  In distributed systems, topology changes create windows where invariants that hold during steady state do not hold; code that assumes "I am always a replica for this key" will break during ring movements.

---

## Test
`ViewUtilsTest.testBaseTokenDoesNotBelongToLocalReplicaShouldReturnEmpty()` added — asserts that `getViewNaturalEndpoint` returns `Optional.empty()` when the local node does not own the given base token.
