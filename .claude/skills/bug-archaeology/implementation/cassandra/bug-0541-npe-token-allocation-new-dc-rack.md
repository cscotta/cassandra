# Bug 0541: NPE in Token Allocation When DC or Rack Not Yet in Topology

**Commit**: 2374a74eba · **Ticket**: CASSANDRA-14592
**Subsystem**: `src/java/org/apache/cassandra/dht/tokenallocator/TokenAllocation.java`
**Category**: Missing null / bounds check

---

## Root Cause

`TokenAllocation.getTokensInLocalGroup` called `topology.getDatacenterRacks().get(dc).asMap().size()` without first checking whether the DC had any entry in the topology map. When `allocate_tokens_for_keyspace` is invoked for a node in a brand-new DC or a DC whose rack has not yet been registered in `TokenMetadata`, `getDatacenterRacks().get(dc)` returns `null`, causing an immediate `NullPointerException`. The fix adds a null check and an additional check that the specific rack is present; if either is absent the code treats the new endpoint as its own isolated unit (rack count = 1) until the topology is populated.

### Before / After

```java
// BEFORE
int racks = topology.getDatacenterRacks().get(dc).asMap().size();
```

```java
// AFTER
int racks = topology.getDatacenterRacks().get(dc) != null
        && topology.getDatacenterRacks().get(dc).containsKey(snitch.getRack(endpoint))
        ? topology.getDatacenterRacks().get(dc).asMap().size()
        : 1;
```

---

## How to Spot

Any call chain of the form `map.get(key).method()` without a null-check on the `get` result is a potential NPE if the key may not exist. In topology/metadata code this pattern is especially risky because DC and rack information is populated lazily as nodes join; code that runs during bootstrap or initial ring setup may execute before the relevant entries exist.

---

## Test

No test added.
