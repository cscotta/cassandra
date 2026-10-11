# Bug 1080: Rack Reassignment Not Propagated to TokenMetadata Topology Cache

**Commit**: 257cdaa08d · **Ticket**: CASSANDRA-10238
**Subsystem**: src/java/org/apache/cassandra/locator/TokenMetadata.java
**Category**: State not cleaned up

---

## Root Cause

`TokenMetadata.Topology` maintained a cached DC/rack mapping for each endpoint but provided no mechanism to update that mapping when a live snitch change reassigned an endpoint to a different rack. When the snitch was reloaded (e.g. via `PropertyFileSnitch` or `YamlFileNetworkTopologySnitch`), the old rack entry was never removed from `dcRacks`, so the topology held phantom entries for the vacated rack. The replication strategy then computed placement using this stale view, potentially over-placing or under-placing replicas and violating the configured RF contract.

### Before / After

```java
// BEFORE — addEndpoint() removed the old rack entry but no updateEndpoint() existed;
// snitch reload never invalidated the cached location
protected void addEndpoint(InetAddress ep)
{
    // only called on first registration, not on snitch change
    ...
}
// Snitch file watcher called only reloadConfiguration(), never touched TokenMetadata
```

```java
// AFTER — new updateEndpoint()/updateEndpoints() methods propagate snitch changes
void updateEndpoint(InetAddress ep, IEndpointSnitch snitch)
{
    Pair<String,String> current = currentLocations.get(ep);
    String dc = snitch.getDatacenter(ep);
    String rack = snitch.getRack(ep);
    if (dc.equals(current.left) && rack.equals(current.right))
        return;
    doRemoveEndpoint(ep, current);
    doAddEndpoint(ep, dc, rack);
}
// Snitch file watchers now call StorageService.instance.updateTopology()
```

---

## How to Spot

Any code that caches derived topology state (DC/rack membership maps) populated at endpoint-registration time but never refreshed on configuration reload is vulnerable to this pattern. Review every `ResourceWatcher` callback that reloads a snitch or topology file: if it only updates the snitch's internal state without propagating the change to consumers that hold their own derivative caches (like `TokenMetadata.Topology`), those caches will silently diverge from reality. The symptom is replication placement violations that appear only after a live rack reassignment, with no errors logged.

---

## Test

`TokenMetadataTest.testTopologyUpdate_RackConsolidation` and `testTopologyUpdate_RackExpansion` verify that calling `updateTopology()` after swapping the snitch implementation correctly migrates endpoints between racks and removes vacated rack entries.
