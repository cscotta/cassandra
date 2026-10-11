## Commit

c612cfff29 — KAFKA-19274: Consumer offsets coordinator ignores resignation when __consumer_offsets partition is deleted

## Ticket

KAFKA-19274

## Subsystem

Group coordinator / metadata publishing

## Category

State not cleaned up

## Root cause

`BrokerMetadataPublisher.scala` called `coordinator.resignation(partitionId, Some(leaderEpoch))` when a `__consumer_offsets` partition was deleted. The coordinator's `resignation` handler only acted when the provided epoch was greater than the currently stored epoch. Partition deletion does not increment the epoch, so the resignation was silently ignored: the coordinator continued to act as the active coordinator for a partition that no longer existed, potentially serving stale group state. The fix changes the call to `coordinator.resignation(partitionId, None)`, which unconditionally triggers coordinator clean-up regardless of epoch ordering.

## Before / After

```scala
// Before — epoch-gated resignation, ignored on partition delete
coordinator.resignation(partitionId, Some(leaderEpoch))

// After — epoch-free resignation, always triggers cleanup
coordinator.resignation(partitionId, None)
```

## How to spot

In metadata publishers that handle partition deletion events, verify that coordinator `resignation` calls use `None`/`Optional.empty()` for epoch rather than the current epoch. Epoch-gated resignations will be silently skipped if the delete path does not advance the epoch.

## Test

`BrokerMetadataPublisherTest` was extended with a case that asserts the coordinator enters the resigned state after a `__consumer_offsets` partition deletion.
