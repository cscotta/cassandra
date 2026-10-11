## Commit

d5ce463ed3 — KAFKA-19253 1/N: Share sessions and partitions not cleaned up when share.version toggled off

## Ticket

KAFKA-19253

## Subsystem

Share groups / feature flag handling

## Category

State not cleaned up

## Root cause

`SharePartitionManager` had no hook into the `share.version` feature-level metadata change events. When an administrator toggled `share.version` off via a metadata update, all active share sessions and share partition state remained in memory. Subsequent requests to those partitions would encounter inconsistent state: the coordinator rejected them as unsupported, but the partition manager still held live cursors and session registrations, leaking memory and potentially causing erroneous state on a subsequent re-enable.

## Before / After

```java
// Before — no feature-toggle listener in SharePartitionManager
// BrokerMetadataPublisher.onMetadataUpdate did not call SharePartitionManager

// After — feature-toggle callback added
public void onShareVersionToggle(boolean enabled) {
    if (!enabled) {
        shareSessionCache.clear();
        sharePartitionMap.clear();
    }
}
// BrokerMetadataPublisher.onMetadataUpdate calls:
sharePartitionManager.onShareVersionToggle(newShareVersion > 0);
```

## How to spot

Search for feature-gated subsystems that maintain in-memory caches or session maps. Each such subsystem must subscribe to the feature's metadata-change event and clear its state when the feature is disabled. Absence of such a callback is a state-leak that will surface after feature flag toggling in rolling upgrades.

## Test

`SharePartitionManagerTest` was extended with a test that toggles the share version off and asserts that the session cache and partition map are emptied.
