# Bug 2375: DynamicEndpointSnitch NPE when an endpoint is in windows but not in penalties map

**Commit**: 0cc97d91c0 · **Ticket**: n/a

## Detection Rule

When two concurrent maps are iterated separately and one is expected to contain all keys present in the other, check whether a race condition or different insertion ordering could leave a key in one map but absent from the other, and add a `containsKey` guard before the lookup.

## Code Smell

`score += penalties.get(entry.getKey()) / maxPenalty` without a prior `containsKey` check will throw a `NullPointerException` (auto-unboxing `null` `Double` to `double`) whenever a host appears in `windows` but has not yet received any penalty entry.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: map-lookup-without-containskey
- severity: crash
- subsystem: network-messaging

## Root Cause

`DynamicEndpointSnitch` maintains two separate `ConcurrentHashMap`s: `windows` (latency samples) and `penalties` (penalty scores). Hosts are added to `windows` when a response is received, but they may not yet have a corresponding entry in `penalties`. During the scoring loop, `penalties.get(key)` returned `null` for new hosts, and auto-unboxing this `null` to `double` for the division threw a `NullPointerException`. The fix checks `containsKey` first and adds `1.0` (equivalent to the maximum normalised penalty) for hosts not yet in the map.

## Example

```java
// BEFORE
for (Map.Entry<InetAddress, BoundedStatsDeque> entry : windows.entrySet()) {
    double score = entry.getValue().mean() / maxLatency;
    score += penalties.get(entry.getKey()) / maxPenalty; // NPE if key absent
}
```

```java
// AFTER
    if (penalties.containsKey(entry.getKey()))
        score += penalties.get(entry.getKey()) / maxPenalty;
    else
        score += 1; // maxPenalty / maxPenalty — treat as worst-case
```
