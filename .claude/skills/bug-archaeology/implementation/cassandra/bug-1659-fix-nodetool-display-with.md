# Bug 1659: Nodetool Ring Display Incorrectly Detects Vnodes in NodeTool.java

**Commit**: 0ad5e3681b · **Ticket**: CASSANDRA-7082
**Subsystem**: src/java/org/apache/cassandra/tools
**Category**: Logic error in condition

---

## Root Cause

The vnode detection heuristic in `NodeTool.java` compared `tokensToEndpoints.values().size()` (the number of endpoint addresses, which may contain duplicates in a `Map.values()` view and equals the number of tokens) against `tokensToEndpoints.keySet().size()` (also the number of tokens). Because both sides of the comparison counted the same quantity, the condition was never true, and `isTokenPerNode` was never set to `false` even on a vnode cluster. The fix moves the check to after `getOwnershipByDc()` builds a deduplicated `dcs` map, where `dcs.values().size()` correctly reflects the number of unique nodes, making the comparison meaningful.

### Before / After

```java
// BEFORE  (tokensToEndpoints.values().size() == tokensToEndpoints.keySet().size() always)
if (tokensToEndpoints.values().size() < tokensToEndpoints.keySet().size())
    isTokenPerNode = false;

Map<String, SetHostStat> dcs = getOwnershipByDc(probe, resolveIp, tokensToEndpoints, ownerships);
```

```java
// AFTER
Map<String, SetHostStat> dcs = getOwnershipByDc(probe, resolveIp, tokensToEndpoints, ownerships);

// More tokens than nodes (aka vnodes)?
if (dcs.values().size() < tokensToEndpoints.keySet().size())
    isTokenPerNode = false;
```

---

## How to Spot

When comparing "number of nodes" to "number of tokens" using a `tokensToEndpoints` map, always use a deduplicated structure (e.g., a `Set` of values or a DC summary map) on the node side. Comparing `map.values().size()` to `map.keySet().size()` on the same map is always a no-op for a `Map` and should raise a review flag.

---

## Test

No test added.
