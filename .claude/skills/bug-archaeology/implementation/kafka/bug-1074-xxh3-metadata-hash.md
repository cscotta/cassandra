## Commit

a1008dc85d — KAFKA-17747 [2/N]: Add XXH3 topic and group hash utilities for metadata fingerprinting

## Ticket

KAFKA-17747

## Subsystem

Group coordinator / metadata fingerprinting

## Category

Missing null / bounds check

## Root cause

The group coordinator had no fast way to detect whether the metadata snapshot seen by a consumer matched the coordinator's current view. Without a stable hash of topic-partition metadata, the coordinator had to do a full deep-equal comparison on every heartbeat, which was O(T×P) in the number of topics and partitions. The absence of a hash function also meant that any metadata change — even a rack-information update — forced a full assignment recomputation with no ability to short-circuit. The fix introduces `computeTopicHash` and `computeGroupHash` using the streaming XXH3 algorithm (via the `hash4j` library) to produce stable 64-bit fingerprints.

## Before / After

```java
// Before — no hash, full deep comparison on every heartbeat
boolean metadataChanged = !currentMetadata.equals(previousMetadata);

// After — O(1) fingerprint comparison
long hash = Utils.computeTopicHash(topicId, partitions, racks);
boolean metadataChanged = (hash != previousHash);
```

## How to spot

Search for `equals` calls on large metadata collections inside heartbeat or assignment paths. Any O(n) equality check that runs on every heartbeat cycle is a scalability problem; replacing it with a hash comparison reduces per-heartbeat cost to O(1) at the cost of occasional hash collisions.

## Test

`UtilsTest` was extended with test vectors for `computeTopicHash` and `computeGroupHash` to verify determinism and collision resistance across known inputs.
