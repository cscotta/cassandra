# Bug 2916: Stale endpoints visible after clearCachedEndpoints due to non-atomic clear

**Commit**: 1768ccd676 · **Ticket**: CASSANDRA-1147

## Detection Rule
When a cache is cleared with `map.clear()` on a non-blocking concurrent map, threads already holding a reference to the map object can still read entries that were logically evicted.

## Code Smell
`cachedEndpoints.clear()` on a `NonBlockingHashMap` races with threads that read from the map between when old entries are removed and when new ones are added; volatile field replacement avoids this window.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: non-atomic-cache-clear
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`AbstractReplicationStrategy.clearCachedEndpoints()` called `cachedEndpoints.clear()`, which could leave a window where threads saw a partially-cleared map or, worse, computed new endpoints that referenced the now-stale token map. By replacing the map reference atomically (assigning a fresh `NonBlockingHashMap` to a `volatile` field), readers either see the old complete map or the new empty one — never a mix.

## Example
```java
// BEFORE
private final Map<EndpointCacheKey, ArrayList<InetAddress>> cachedEndpoints;
// ...
protected void clearCachedEndpoints() {
    cachedEndpoints.clear();  // non-atomic; concurrent readers may see partial state
}
```
```java
// AFTER
private volatile Map<EndpointCacheKey, ArrayList<InetAddress>> cachedEndpoints;
// ...
protected void clearCachedEndpoints() {
    cachedEndpoints = new NonBlockingHashMap<>();  // atomic reference swap
}
```
