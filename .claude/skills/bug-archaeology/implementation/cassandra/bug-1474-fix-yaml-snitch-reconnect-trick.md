# Bug 1474: Wrong IP String Representation in YamlFileNetworkTopologySnitch

**Commit**: cb1479235b · **Ticket**: CASSANDRA-7912
**Subsystem**: src/java/org/apache/cassandra/locator/YamlFileNetworkTopologySnitch.java
**Category**: Wrong constant / default value

---

## Root Cause

`YamlFileNetworkTopologySnitch` used `InetAddress.toString()` to serialize the node's internal IP into gossip state, which produces a string in the format `/127.0.0.1` (with a leading slash) rather than a bare IP address like `127.0.0.1`. The comparison to detect whether gossip state needed updating also used `toString()`, so the check was self-consistent — but the stored value was never equal to values stored by other code paths that used `getHostAddress()`. This prevented the "reconnect trick" from working: the node would always see a mismatch, repeatedly broadcasting a malformed INTERNAL_IP value, or fail to propagate the correct one.

### Before / After

```java
// BEFORE
if ((vv != null && !vv.value.equals(localNodeData.dcLocalAddress.toString()))
    || vv == null)
{
    Gossiper.instance.addLocalApplicationState(ApplicationState.INTERNAL_IP,
        StorageService.instance.valueFactory.internalIP(localNodeData.dcLocalAddress.toString()));
}
```

```java
// AFTER
if ((vv != null && !vv.value.equals(localNodeData.dcLocalAddress.getHostAddress()))
    || vv == null)
{
    Gossiper.instance.addLocalApplicationState(ApplicationState.INTERNAL_IP,
        StorageService.instance.valueFactory.internalIP(localNodeData.dcLocalAddress.getHostAddress()));
}
```

---

## How to Spot

When an `InetAddress` is serialized to a string for storage or comparison, always use `getHostAddress()` (returns `"1.2.3.4"`) rather than `toString()` (returns `"/1.2.3.4"`). Review any use of `.toString()` on `InetAddress` objects for off-by-slash bugs in equality checks or gossip/protocol messages.

---

## Test

No test added.
