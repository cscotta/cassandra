# Bug 3082: EACH_QUORUM writes throw UnavailableException when DC has exactly enough replicas

**Commit**: 0b31efe296 · **Ticket**: CASSANDRA-3082

## Detection Rule
When checking whether a datacenter has enough replicas for a write, use `<` (strict less-than) rather than `!=` to compare available endpoint count against the required count, because endpoints can exceed the requirement.

## Code Smell
Code that throws `UnavailableException` when `dcEndpoints.get(dc).get() != responses.get(dc).get()` is wrong when the live endpoint count can legally exceed the quorum threshold.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: inequality-check-should-be-less-than
- severity: crash
- subsystem: paxos-lwt

## Root Cause
`DatacenterSyncWriteResponseHandler.assureSufficientLiveNodes()` compared the number of available DC endpoints to the number of required responses using `!=`. When a DC had more live replicas than the quorum requirement (e.g. 3 live vs 2 required), `dcEndpoints != responses` was true even though the DC was perfectly healthy, incorrectly throwing `UnavailableException`. The fix changes the comparison to `<`.

## Example
```java
// BEFORE
if (dcEndpoints.get(dc).get() != responses.get(dc).get())
    throw new UnavailableException();
```
```java
// AFTER
if (dcEndpoints.get(dc).get() < responses.get(dc).get())
    throw new UnavailableException();
```
