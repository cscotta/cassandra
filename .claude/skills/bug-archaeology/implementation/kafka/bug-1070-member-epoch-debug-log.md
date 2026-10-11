## Commit

8a577fa5af — MINOR: Add debug log for member epoch update and test for rebalance fence recovery

## Ticket

MINOR

## Subsystem

Clients / new consumer protocol

## Category

Missing null / bounds check

## Root cause

`CommitRequestManager` silently updated the member epoch without any log output, making it difficult to trace epoch transitions during debugging. Separately, `PlaintextConsumerPollTest` lacked a test verifying that a consumer could recover its epoch after receiving a `FENCED_MEMBER_EPOCH` error caused by a delayed rebalance. Without this test, regressions in the recovery path (where the member must rejoin with epoch 0) would be invisible in CI.

## Before / After

```java
// Before — silent epoch update
this.memberEpoch = epoch;

// After — debug-level log on update
log.debug("Member epoch updated from {} to {}", this.memberEpoch, epoch);
this.memberEpoch = epoch;
```

## How to spot

Search for epoch or generation fields that are assigned without any log statement. In distributed consensus code, epoch transitions are critical events; silent updates make production incident investigation significantly harder.

## Test

`PlaintextConsumerPollTest` was extended with `testConsumerRecoveryAfterDelayedRebalanceFence` to verify the consumer re-establishes membership after a fencing event.
