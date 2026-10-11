# Bug 2413: Bootstrap/rebuild streams from multiple replicas for the same range

**Commit**: 271630d585 · **Ticket**: CASSANDRA-3922

## Detection Rule

When a loop iterates replica candidates to find a source for a range and sets `foundSource = true` without breaking, all subsequent replicas for that range are also added as sources, causing duplicate streaming.

## Code Smell

A `for` loop over endpoints sets `foundSource = true` and `rangeFetchMapMap.put(address, range)` but does not `break` — every acceptable endpoint becomes a source for the same range.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: missing-break-after-match
- severity: performance
- subsystem: bootstrap-startup

## Root Cause

`RangeStreamer` iterates all replica endpoints to pick a source for each range during bootstrap or rebuild. Once a suitable source was found and recorded, the loop should stop. Without a `break`, every subsequent replica was also added to `rangeFetchMapMap`, causing the same data range to be streamed from multiple nodes simultaneously — wasting bandwidth and potentially causing conflicts. The fix adds `break` after recording a source.

## Example

```java
// BEFORE — loops over all replicas, adds all of them as sources
for (InetAddress source : sources) {
    if (isAcceptable(source)) {
        rangeFetchMapMap.put(source, range);
        foundSource = true;
        // no break — keeps adding more sources for same range
    }
}
```

```java
// AFTER
for (InetAddress source : sources) {
    if (isAcceptable(source)) {
        rangeFetchMapMap.put(source, range);
        foundSource = true;
        break;  // stream each range from exactly one source
    }
}
```
