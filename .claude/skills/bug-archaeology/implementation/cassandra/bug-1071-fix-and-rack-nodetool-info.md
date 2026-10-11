# Bug 1071: nodetool info Shows Wrong DC and Rack

**Commit**: 6039d0ef74 · **Ticket**: CASSANDRA-10382
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong constant / default value

---

## Root Cause

`NodeProbe.getEndpoint()` needed to look up the local node's IP address given its host ID. To do this it called `ssProxy.getHostIdMap()`, which returned an endpoint-to-hostId mapping (i.e. the reverse of what was needed), and then used the local host ID as the lookup key. Because the key direction was inverted, the lookup always missed and `getEndpoint()` returned `null`. `nodetool info` uses `getEndpoint()` to determine which DC and Rack entry to display, so both fields appeared blank or incorrect. The fix introduces a dedicated `getHostIdToEndpoint()` method that returns the correctly-oriented hostId→endpoint map and updates the call site to use it.

### Before / After

```java
// BEFORE — getHostIdMap() returns endpoint→hostId; lookup by hostId always misses
public String getEndpoint()
{
    Map<String, String> hostIdToEndpoint = ssProxy.getHostIdMap();  // wrong direction
    return hostIdToEndpoint.get(ssProxy.getLocalHostId());
}
```

```java
// AFTER — getHostIdToEndpoint() returns hostId→endpoint; lookup succeeds
public String getEndpoint()
{
    Map<String, String> hostIdToEndpoint = ssProxy.getHostIdToEndpoint();
    return hostIdToEndpoint.get(ssProxy.getLocalHostId());
}
```

---

## How to Spot

Map-direction errors are easy to introduce when both keys and values are the same type (here, both are `String`). When reviewing a map lookup, confirm that the key used at the call site matches the key type of the map, not the value type. A method named `getHostIdMap` is ambiguous about direction; the fix renames the methods to `getEndpointToHostId` and `getHostIdToEndpoint`, making the direction explicit in the name and making future mistakes easier to catch.

---

## Test

No test added.
