# Bug 2508: Efficient cross-DC writes permanently disabled due to inverted condition

**Commit**: 61109407dd · **Ticket**: CASSANDRA-3472

## Detection Rule
When a volatile boolean is set by iterating gossip state and short-circuits on a missing entry, verify that its initial value is correct and that the condition is not inverted relative to the intent.

## Code Smell
`if (dataCenter.equals(localDataCenter) || StorageService.instance.useEfficientCrossDCWrites())` — the `efficientCrossDCWrites` flag starts as `false` and is only set to `true` after all nodes gossip their version, so any node that joins before the scan completes leaves the flag `false` forever, causing the else-branch (efficient forwarding) to be skipped for all remote DCs.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: stale-flag-inverted-default
- severity: performance
- subsystem: network-messaging

## Root Cause
`updateEfficientCrossDCWriteMode()` initialized `efficientCrossDCWrites` to `false` and only set it to `true` once all nodes in gossip had a `RELEASE_VERSION` state. In practice the flag would remain `false` whenever any node hadn't yet gossiped its version, which meant the StorageProxy would always use direct per-node writes (the local-DC path) instead of the efficient single-coordinator-per-DC forwarding. The fix removed the obsolete compatibility logic entirely since mixing very old (pre-0.7.1) nodes with 0.8+ was no longer supported.

## Example
```java
// BEFORE
if (dataCenter.equals(localDataCenter)
        || StorageService.instance.useEfficientCrossDCWrites())
{
    // direct writes
}
// else: efficient forwarding (almost never reached)
```
```java
// AFTER
if (dataCenter.equals(localDataCenter))
{
    // direct writes to local DC only
}
// else: always use efficient cross-DC forwarding
```
