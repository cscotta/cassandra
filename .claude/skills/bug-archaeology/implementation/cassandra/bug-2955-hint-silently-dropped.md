# Bug 2955: Hinted handoff gives up entirely when no alternate endpoint found

**Commit**: e659b06f3a · **Ticket**: CASSANDRA-496

## Detection Rule
When a hinted-handoff hint-routing loop finds no suitable live alternate endpoint, check whether it silently discards the hint instead of falling back to a local store.

## Code Smell
`getHintedMapForEndpoints` logs a warning "Unable to find a live Endpoint" and adds nothing to the map when all candidate alternate endpoints are already used, causing the hint to be silently dropped rather than stored locally.

## Tags
- scope: cassandra
- category: silent-skip
- code-pattern: hint-dropped-on-no-alternate
- severity: data-loss
- subsystem: storage-engine

## Root Cause
When a natural endpoint is down and every candidate alternate endpoint is already in the hint map, the old code logged a warning and did nothing — the hint was not stored anywhere. This could cause data loss for writes that could not find a live endpoint to store the hint. The fix falls back to storing the hint on the local node (`StorageService.getLocalControlEndPoint()`) when no unused live alternate endpoint exists, saving the network trip while preserving the hint data.

## Example
```java
// BEFORE: hint silently dropped
if (endPoint != null) {
    map.put(endPoint, topN[i]);
} else {
    logger_.warn("Unable to find a live Endpoint ...");
    // hint not stored anywhere
}
```
```java
// AFTER: fall back to local node
if (hintLocation == null)
    hintLocation = StorageService.getLocalControlEndPoint();
map.put(hintLocation, ep);
usedEndpoints.add(hintLocation);
```
