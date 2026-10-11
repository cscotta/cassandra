# Bug 2119: ConcurrentModificationException in getBootstrapSource due to unsynchronized map iteration

**Commit**: a7da0295d5 · **Ticket**: CASSANDRA-5170
**Subsystem**: src/java/org/apache/cassandra/service/LoadBroadcaster.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`LoadBroadcaster` stored node load information in a plain `HashMap`. Gossip callbacks (running on gossip threads) called `onApplicationState` to update the map concurrently while `getBootstrapSource` (running on a different thread) iterated over it to find a bootstrap source. This produced a `ConcurrentModificationException`. The fix replaces `HashMap` with `ConcurrentHashMap` so updates are thread-safe, and wraps the return value of `getLoadInfo()` in `Collections.unmodifiableMap()` to prevent external callers from modifying the live map.

### Before / After

```java
// BEFORE
private Map<InetAddress, Double> loadInfo_ = new HashMap<InetAddress, Double>();

public Map<InetAddress, Double> getLoadInfo()
{
    return loadInfo_;
}
```

```java
// AFTER
private ConcurrentMap<InetAddress, Double> loadInfo_ = new ConcurrentHashMap<InetAddress, Double>();

public Map<InetAddress, Double> getLoadInfo()
{
    return Collections.unmodifiableMap(loadInfo_);
}
```

---

## How to Spot

Any shared mutable collection updated from gossip or network callbacks and read from other threads without synchronization is a CME candidate. Look for `HashMap` or `ArrayList` fields on singleton services that receive gossip state-change notifications. Prefer `ConcurrentHashMap` for such shared maps.

---

## Test

No test added.
