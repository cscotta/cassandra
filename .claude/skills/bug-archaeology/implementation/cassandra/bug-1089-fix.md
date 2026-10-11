# Bug 1089: Fix MaterializedView Natural Endpoint Lookup for Pending Nodes

**Commit**: c7557bdecd · **Ticket**: n/a
**Subsystem**: db/view/MaterializedViewUtils.java
**Category**: Missing null / bounds check

---

## Root Cause

`MaterializedViewUtils.getViewNaturalEndpoint` threw a `RuntimeException` ("Trying to get the view natural endpoint on a non-data replica") whenever the local node was not yet in `localBaseEndpoints`. This could happen legitimately during bootstrap: the node is a *pending* endpoint and therefore not yet in the committed ring, but it still needs to participate in materialized-view writes. The fix detects this case by checking whether there are pending endpoints for the view token; if so, it returns the local broadcast address so hints are routed through the batchlog rather than crashing.

### Before / After

```java
// BEFORE
if (baseIdx < 0)
    throw new RuntimeException("Trying to get the view natural endpoint on a non-data replica");
```

```java
// AFTER
if (baseIdx < 0)
{
    if (StorageService.instance.getTokenMetadata().pendingEndpointsFor(viewToken, keyspaceName).size() > 0)
    {
        // Since there are pending endpoints we are going to store hints in the batchlog regardless.
        return FBUtilities.getBroadcastAddress();
    }
    throw new RuntimeException("Trying to get the view natural endpoint on a non-data replica");
}
```

---

## How to Spot

Whenever code calls `indexOf` (or similar lookup) on a list derived from ring membership and treats a negative result as unconditionally fatal, consider whether the node could legitimately be absent during a transient topology state such as bootstrap or decommission. A missing null/bounds guard on ring-membership lookups is a common source of spurious exceptions during cluster operations.

---

## Test

No test added.
