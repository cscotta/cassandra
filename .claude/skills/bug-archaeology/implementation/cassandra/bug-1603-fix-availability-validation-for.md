# Bug 1603: Fix Availability Validation for LOCAL_ONE CL

**Commit**: ea26dbd730 · **Ticket**: CASSANDRA-7319
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Missing null / bounds check

---

## Root Cause

`ConsistencyLevel.assureEnoughLiveNodesForEachDC()` (the availability check method in `ConsistencyLevel.java`) had `case` branches for `ANY`, `LOCAL_QUORUM`, and `EACH_QUORUM`, but no branch for `LOCAL_ONE`. Without a matching case the `switch` fell through to the default, which performed no local-endpoint check at all. As a result, a write or read at `LOCAL_ONE` would never throw `UnavailableException` even when every node in the local datacenter was down, silently accepting the request.

### Before / After

```java
// BEFORE
case ANY:
    // local hint is acceptable, and local node is always live
    break;
case LOCAL_QUORUM:
    int localLive = countLocalEndpoints(liveEndpoints);
    ...
```

```java
// AFTER
case ANY:
    // local hint is acceptable, and local node is always live
    break;
case LOCAL_ONE:
    if (countLocalEndpoints(liveEndpoints) == 0)
        throw new UnavailableException(this, 1, 0);
    break;
case LOCAL_QUORUM:
    int localLive = countLocalEndpoints(liveEndpoints);
    ...
```

---

## How to Spot

In `switch` statements over enum values, verify that every meaningful enum constant has an explicit case. `LOCAL_ONE` semantically parallels `LOCAL_QUORUM` and `EACH_QUORUM` in requiring a local-DC check, so the absence of its case stands out once the full list of local-aware consistency levels is enumerated.

---

## Test

No test added.
