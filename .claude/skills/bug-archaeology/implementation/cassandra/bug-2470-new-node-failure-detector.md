# Bug 2470: New nodes never reported to FailureDetector, treated as permanently down

**Commit**: 0860058eda · **Ticket**: CASSANDRA-3626

## Detection Rule

When a new endpoint is first seen in gossip and its state is not ALIVE, if `FailureDetector.report()` is never called for it, the failure detector has no arrival samples and will permanently consider the node dead.

## Code Smell

A code path handling "new node" in gossip state processing has only a comment but no `FailureDetector.instance.report(ep)` call before delegating to `handleMajorStateChange`.

## Tags

- scope: cassandra
- category: silent-skip
- code-pattern: missing-failure-detector-registration
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause

In `Gossiper.applyStateLocally()`, when a brand new endpoint was encountered (no prior `EndpointState`), `FailureDetector.instance.report(ep)` was not called. Without at least one arrival window sample, the failure detector would always convict the node as dead. The fix adds `FailureDetector.instance.report(ep)` before `handleMajorStateChange` for new endpoints.

## Example

```java
// BEFORE
else
{
    // this is a new node
    handleMajorStateChange(ep, remoteState);
}
```

```java
// AFTER
else
{
    // this is a new node, report it to the FD in case it is the first time we are seeing it AND it's not alive
    FailureDetector.instance.report(ep);
    handleMajorStateChange(ep, remoteState);
}
```
