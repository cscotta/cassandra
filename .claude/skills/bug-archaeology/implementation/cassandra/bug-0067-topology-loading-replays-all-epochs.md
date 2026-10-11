# Bug 0067: Accord Topology Loading Replays All Topologies Instead of Initializing from Latest

**Commit**: 3324e23f64 · **Ticket**: CASSANDRA-20294
**Subsystem**: `service/accord/AccordService`
**Category**: Logic error in condition

---

## Root Cause

On startup, `AccordService` rebuilt command-store shard state by calling `CommandStores.restoreShardStateUnsafe`, which replayed *all* topology entries stored in the journal, and then started `configService` before fetching missing remote topologies. This meant topologies were reported before the configuration service was ready to handle them, and in the case of a node that had seen many topology changes, the full replay was both wasteful and incorrect: intermediate topologies created stale shard assignments. The fix loads only the *last* persisted topology via `initializeTopologyUnsafe` (constructing shards once from the definitive snapshot), delays `configService.start()` until after all topology reporting is complete, and fetches only missing remote epochs.

### Before / After

```java
// BEFORE – replays all topologies, starts configService too early
node.commandStores().restoreShardStateUnsafe(topology -> configService.reportTopology(topology, true, true));
configService.start();
// ... then fetches remote topologies
```

```java
// AFTER – initialize from last snapshot; start configService only after all epochs are loaded
if (lastSeen != null)
    node.commandStores().initializeTopologyUnsafe(lastSeen);

List<Topology> remote = fetchTopologies(minEpoch, metadata);
Streams.concat(local.stream(), remote.stream())
       .forEach(configService::reportTopology);

ClusterMetadataService.instance().log().addListener(configService.listener);
// ... catch up on any epoch updates that arrived during init
configService.start();
```

---

## How to Spot

Bootstrap and restart code that rebuilds distributed state by replaying a historical log should identify and apply only the *most recent consistent snapshot* rather than re-applying every intermediate entry. When the log is sorted, applying the last entry is sufficient; re-applying all entries can corrupt state if intermediate steps are not idempotent. Additionally, service components that depend on state being fully loaded must be started *after* that loading is complete to avoid processing callbacks against a partially initialized structure.

---

## Test

`AccordBootstrapTest` un-ignored so it validates the topology loading path on bootstrap; no new dedicated regression test was added.
