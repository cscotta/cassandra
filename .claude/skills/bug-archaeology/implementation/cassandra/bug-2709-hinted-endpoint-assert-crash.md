# Bug 2709: Hinted-endpoint availability check crashes with assertion instead of UnavailableException

**Commit**: 15a0f2b207 · **Ticket**: CASSANDRA-2514

## Detection Rule
When iterating a superset collection to count live nodes, skip entries that do not belong to the target subset instead of asserting they must.

## Code Smell
`hintedEndpoints` is a superset of `writeEndpoints` (it may include hint-only nodes), yet the code asserts `writeEndpoints.contains(destination)` — an assertion that fires for valid hint-only destinations and throws `AssertionError` instead of `UnavailableException`.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: assert-instead-of-guard
- severity: crash
- subsystem: network-messaging

## Root Cause
`hintedEndpoints` maps every destination (both real replicas and hint targets) to the original endpoint, while `writeEndpoints` contains only the live replicas. When a replica is down and a hint node is selected, the assertion `writeEndpoints.contains(destination)` fails for the hint node, crashing the thread instead of reaching the `UnavailableException` check. The fix replaces the assert with a conditional `if (writeEndpoints.contains(destination))` so hint-only entries are skipped, allowing the code to correctly count zero live DC nodes and throw `UnavailableException`. The same class of bug was present in both `DatacenterSyncWriteResponseHandler` and `DatacenterWriteResponseHandler`.

## Example
```java
// BEFORE — assertion fires for hint-only destinations
for (InetAddress destination : hintedEndpoints.keySet())
{
    assert writeEndpoints.contains(destination);
    String destinationDC = snitch.getDatacenter(destination);
    dcEndpoints.get(destinationDC).incrementAndGet();
}
```
```java
// AFTER — hint-only destinations are silently skipped
for (InetAddress destination : hintedEndpoints.keySet())
{
    if (writeEndpoints.contains(destination))
    {
        String destinationDC = snitch.getDatacenter(destination);
        dcEndpoints.get(destinationDC).incrementAndGet();
    }
}
```
