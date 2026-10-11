# Bug 1660: Nodetool Ring Display Incorrectly Detects Vnodes in NodeCmd.java

**Commit**: 63ba5886e3 · **Ticket**: CASSANDRA-7082
**Subsystem**: src/java/org/apache/cassandra/tools
**Category**: Logic error in condition

---

## Root Cause

The same vnode detection logic error present in `NodeTool.java` (see Bug 1659) also existed in the older `NodeCmd.java` implementation. The check compared `tokensToEndpoints.values().size()` to `tokensToEndpoints.keySet().size()`, both of which equal the number of token-to-endpoint mappings, so the condition was never true on a vnode cluster. The fix moves the check to after `getOwnershipByDc()` so that `dcs.values().size()` (unique nodes) is compared against the token count.

### Before / After

```java
// BEFORE
if (tokensToEndpoints.values().size() < tokensToEndpoints.keySet().size())
    isTokenPerNode = false;

Map<String, SetHostStat> dcs = getOwnershipByDc(resolveIp, tokensToEndpoints, ownerships);
```

```java
// AFTER
Map<String, SetHostStat> dcs = getOwnershipByDc(resolveIp, tokensToEndpoints, ownerships);

// More tokens than nodes (aka vnodes)?
if (dcs.values().size() < tokensToEndpoints.keySet().size())
    isTokenPerNode = false;
```

---

## How to Spot

Identical pattern to Bug 1659: comparing `map.values().size()` to `map.keySet().size()` on the same map is a tautology for standard `Map` implementations. Any such comparison in vnode-detection logic is almost certainly a copy-paste defect.

---

## Test

No test added.
