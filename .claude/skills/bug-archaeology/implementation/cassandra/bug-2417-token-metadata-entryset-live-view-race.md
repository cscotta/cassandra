# Bug 2417: TokenMetadata.entrySet exposes internal map without lock, causing ConcurrentModificationException

**Commit**: 79050449e7 · **Ticket**: CASSANDRA-3417

## Detection Rule

When a method returns a live view (`entrySet()`, `keySet()`, `values()`) of an internal map that is guarded by a lock, callers that iterate the view without holding the lock can see concurrent modifications — return a defensive copy instead.

## Code Smell

`public Set<Map.Entry<Token,InetAddress>> entrySet() { return tokenToEndpointMap.entrySet(); }` — returns a live backed view of the internal map; any caller that iterates this set without holding `TokenMetadata`'s read lock races with writers that hold the write lock.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: live-view-escapes-lock
- severity: crash
- subsystem: gossip

## Root Cause

`TokenMetadata.entrySet()` returned a live `Set` view of the internal `tokenToEndpointMap`. `NetworkTopologyStrategy.calculateNaturalEndpoints` iterated this set outside of any lock, while concurrent gossip updates could modify the map under the write lock, triggering `ConcurrentModificationException`. The fix removes `entrySet()` and adds `getTokenToEndpointMapForReading()` which copies the map under the read lock, returning a snapshot safe to iterate without a lock. A second snapshot method `getNormalAndBootstrappingTokenToEndpointMap()` (renamed from `getTokenToEndpointMap()`) similarly returns a copy.

## Example

```java
// BEFORE — live view escapes lock, racy iteration
public Set<Map.Entry<Token,InetAddress>> entrySet() {
    return tokenToEndpointMap.entrySet();   // backed by live map, no lock
}
// caller:
for (Entry<Token, InetAddress> e : tokenMetadata.entrySet()) { ... }  // CME possible
```

```java
// AFTER — defensive copy under read lock
public Map<Token, InetAddress> getTokenToEndpointMapForReading() {
    lock.readLock().lock();
    try {
        Map<Token, InetAddress> map = new HashMap<>(tokenToEndpointMap.size());
        map.putAll(tokenToEndpointMap);
        return map;
    } finally { lock.readLock().unlock(); }
}
// caller:
for (Entry<Token, InetAddress> e : tokenMetadata.getTokenToEndpointMapForReading().entrySet()) { ... }
```
