# Bug 2992: Failure detector arrival window initialized with zero, skewing mean arrival time

**Commit**: 9292af8860 · **Ticket**: CASSANDRA-13

## Detection Rule
When recording the first inter-arrival interval for a failure detector, initialize it to a reasonable estimate (such as half the gossip interval) rather than zero, so the initial mean is not skewed toward zero.

## Code Smell
An `interArrivalTime` variable is initialized to `0` before a conditional that only sets it when `tLast_ > 0`; on the very first call `tLast_` is 0, the condition is false, and `0` is recorded as the first interval, poisoning the rolling mean.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: zero-default-skews-average
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`ArrivalWindow.add()` set `interArrivalTime = 0` as a default. On the first heartbeat `tLast_` is 0 so the branch that computes a real inter-arrival delta is skipped, and 0 is pushed into the sliding window. This made the mean arrival time extremely low at startup, causing the phi-accrual failure detector to consider all nodes as failed immediately after they first appeared. The fix uses `Gossiper.intervalInMillis_ / 2` as a seed for the first interval.

## Example
```java
// BEFORE: zero seed makes mean too small, nodes appear dead
double interArrivalTime = 0;
if (tLast_ > 0L) {
    interArrivalTime = (value - tLast_);
}

// AFTER: seed with half the gossip period
double interArrivalTime;
if (tLast_ > 0L) {
    interArrivalTime = (value - tLast_);
} else {
    interArrivalTime = Gossiper.intervalInMillis_ / 2;
}
```
