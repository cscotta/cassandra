# Bug 2028: Ec2Snitch Returns Wrong DC/Rack for Endpoints Not Yet in Gossip

**Commit**: 59bd0e7169 · **Ticket**: CASSANDRA-5432
**Subsystem**: src/java/org/apache/cassandra/locator/Ec2Snitch.java, src/java/org/apache/cassandra/net/OutboundTcpConnectionPool.java
**Category**: Wrong constant / default value

---

## Root Cause

A prior refactor (CASSANDRA-5171) moved DC/rack persistence out of `Ec2Snitch` and into a base class, but the fallback path in `Ec2Snitch` that loaded DC/rack from `SystemTable` was left in place. Because the base class no longer populated that saved-endpoints map correctly, the snitch would return stale or incorrect DC/rack values for endpoints that had not yet appeared in gossip. Additionally, `OutboundTcpConnectionPool.endPoint()` did not special-case the local broadcast address, causing a connection to itself to use the broadcast IP instead of the loopback/local address.

The fix removes the now-dead `savedEndpoints` fallback from `Ec2Snitch` so unknown endpoints return the safe default (`UNKNOWN-DC`/`UNKNOWN-RACK`), and adds a short-circuit in `endPoint()` so self-connections use `FBUtilities.getLocalAddress()`.

### Before / After

```java
// BEFORE (Ec2Snitch.getRack)
if (state == null || state.getApplicationState(ApplicationState.RACK) == null)
{
    if (savedEndpoints == null)
        savedEndpoints = SystemTable.loadDcRackInfo();
    if (savedEndpoints.containsKey(endpoint))
        return savedEndpoints.get(endpoint).get("rack");
    return DEFAULT_RACK;
}
```

```java
// AFTER
if (state == null || state.getApplicationState(ApplicationState.RACK) == null)
    return DEFAULT_RACK;
```

```java
// AFTER (OutboundTcpConnectionPool.endPoint)
if (id.equals(FBUtilities.getBroadcastAddress()))
    return FBUtilities.getLocalAddress();
return resetedEndpoint == null ? id : resetedEndpoint;
```

---

## How to Spot

When a refactor removes the code that populates a cache or fallback store, check every consumer of that store. Leftover fallback paths that read from a never-populated structure silently return wrong data. Dead fallbacks should be removed alongside the population code.

---

## Test

No test added.
