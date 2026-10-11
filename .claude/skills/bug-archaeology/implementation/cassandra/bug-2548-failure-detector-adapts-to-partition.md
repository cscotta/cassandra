# Bug 2548: FailureDetector adapts to long partitions and delays marking hosts down

**Commit**: 45dbeb7d30 · **Ticket**: CASSANDRA-3273

## Detection Rule
When the phi-accrual failure detector records arbitrarily long inter-arrival intervals during a network partition, the adaptive mean shifts upward and the detector requires proportionally longer silence before it crosses the conviction threshold.

## Code Smell
`ArrivalWindow.add()` unconditionally records every inter-arrival interval including those caused by multi-second partitions. The `clear()` method is absent from the `IFailureDetector` interface, so stale measurements cannot be discarded after a node reboots.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: unbounded-sample-window
- severity: hang
- subsystem: gossip

## Root Cause
During a long network partition the phi detector accumulated very large heartbeat intervals in its `BoundedStatsDeque`. These inflated the rolling mean, causing subsequent phi values to fall below `phiConvictThreshold` even when the node was genuinely unreachable, delaying or preventing conviction. The fix caps recorded intervals at `getRpcTimeout()` and clears the window when a previously-dead node's generation changes (indicating a reboot), so stale large samples cannot mask a new outage.

## Example
```java
// BEFORE: every interval recorded regardless of size
interArrivalTime = (value - tLast_);
tLast_ = value;
arrivalIntervals_.add(interArrivalTime);
```
```java
// AFTER: cap at rpc timeout to avoid detector adaptation
if (interArrivalTime <= MAX_INTERVAL_IN_MS)
    arrivalIntervals_.add(interArrivalTime);
else
    logger_.debug("Ignoring interval time of {}", interArrivalTime);
tLast_ = value;
```
