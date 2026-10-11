# Bug 2338/2339/2340: Streaming session IDs generated from System.nanoTime() can collide between nodes

**Commit**: edbd35a322 (1.1.x) / bbe105cfa5 (1.1.x) / 2db61ab9e0 (1.0.x) · **Ticket**: CASSANDRA-4223

## Detection Rule

When generating session or connection IDs that must be unique across multiple JVM instances on different machines, do not use `System.nanoTime()` — it is JVM-local and multiple nodes can produce the same value.

## Code Smell

`Pair<InetAddress, Long>(host, System.nanoTime())` as a session context key, where two different nodes talking to the same peer can both generate the same `nanoTime()` value, causing one session to overwrite the other in the `ConcurrentMap`.

## Tags

- scope: distributed-systems
- category: race-condition
- code-pattern: nanotime-as-distributed-unique-id
- severity: data-loss
- subsystem: streaming

## Root Cause

`StreamInSession` and `StreamOutSession` keyed sessions with `(peer, nanoTime())`. Two nodes establishing simultaneous streaming sessions to a common third node could generate identical nanoTime values, causing one session's entry in the `ConcurrentMap` to silently replace the other. Files from the overwritten session would never be tracked, leading to incomplete streaming and potential data loss during repair or bootstrap. The fix uses a JVM-local `AtomicInteger` counter combined with a flag bit (high 32 bits) encoding whether the session originated as StreamIn or StreamOut, guaranteeing uniqueness within a node and preventing cross-directional collisions with the peer's counter.

## Example

```java
// BEFORE: nanoTime can collide across machines
public static StreamInSession create(InetAddress host, IStreamCallback callback)
{
    Pair<InetAddress, Long> context = new Pair<>(host, System.nanoTime());  // collision risk
    // ...
}
```

```java
// AFTER: monotonic counter with direction flag, collision-free
private static long nextSessionId()
{
    return (((long)StreamHeader.STREAM_IN_SOURCE_FLAG << 32) + sessionIdCounter.incrementAndGet());
}

public static StreamInSession create(InetAddress host, IStreamCallback callback)
{
    Pair<InetAddress, Long> context = new Pair<>(host, nextSessionId());
    // ...
}
```
