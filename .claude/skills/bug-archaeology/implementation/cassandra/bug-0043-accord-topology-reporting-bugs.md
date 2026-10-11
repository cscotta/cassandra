# Bug 0043: Accord Topology Reporting Race and Missing Gap-Fill

**Commit**: d516ea2f0f · **Ticket**: CASSANDRA-20730
**Subsystem**: service/accord/AccordService, AccordConfigurationService
**Category**: Race condition / missing synchronization

---

## Root Cause

Two related bugs existed in Accord's topology initialization. First, for single-node clusters, the current topology was reported to `AccordConfigurationService` before the TCM event listener was registered. This created a race: if the epoch advanced between the report and the listener registration, the new epoch was silently dropped and the configuration service would never see it. Second, in `AccordConfigurationService.checkIfNodesRemoved`, the guard `epochs.minEpoch() == topology.epoch()` failed to handle the epoch-zero case (no epochs yet registered) and also used `==` instead of `<=`, meaning that node-removal notifications were suppressed incorrectly for multi-epoch ranges. The fix reorders initialization to subscribe the listener before reporting the current topology, and adds gap-filling logic in the timeout loop.

### Before / After

```java
// BEFORE — topology reported before listener registered (race)
else if (images.isEmpty()) // First boot, single-node cluster
    configService.reportTopology(AccordTopology.createAccordTopology(metadata));
ClusterMetadataService.instance().log().addListener(configService.listener);
// ... gap-fill only done once after subscribing
```

```java
// AFTER — listener registered first, then topology reported
ClusterMetadataService.instance().log().addListener(configService.listener);
// Report current topology AFTER subscribing
if (remote == null && images.isEmpty())
    configService.reportTopology(AccordTopology.createAccordTopology(metadata));
```

```java
// BEFORE — faulty guard in checkIfNodesRemoved
if (epochs.minEpoch() == topology.epoch()) return;
```

```java
// AFTER — handles epoch-zero and uses <= instead of ==
long minEpoch = epochs.minEpoch();
if (minEpoch == 0 || topology.epoch() <= minEpoch) return;
```

---

## How to Spot

When an event listener is registered after an initial state snapshot is reported, there is always a TOCTOU window. The correct pattern is to register the listener first, then read the current state, so any concurrent updates are either delivered via the listener or will be observed in the current-state read. Review all "subscribe then snapshot" patterns and ensure the order is reversed to "snapshot then verify, with listener already active."

---

## Test

No dedicated test added; this is a startup race condition that requires multi-threaded integration testing to reproduce reliably.
