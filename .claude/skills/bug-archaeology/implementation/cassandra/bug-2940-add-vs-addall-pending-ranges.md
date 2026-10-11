# Bug 2940: add() instead of addAll() for pending-range endpoints loses bootstrap nodes

**Commit**: 82d843bad8 · **Ticket**: CASSANDRA-603

## Detection Rule
When a map value type changes from a single element to a `Collection`, all callers that previously used `endpoints.add(entry.getValue())` must be updated to `endpoints.addAll(entry.getValue())` or only the first element is included.

## Code Smell
`endpoints.add(entry.getValue())` where `entry.getValue()` is a `Collection<InetAddress>`; Java's `List.add(Object)` adds the entire collection as a single element (or fails to compile) instead of adding each address individually.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: add-vs-addAll-collection
- severity: silent-wrong-result
- subsystem: bootstrap-startup

## Root Cause
`AbstractReplicationStrategy.getNaturalEndpoints()` iterated over pending ranges and called `endpoints.add(entry.getValue())` to include nodes that are bootstrapping into a range. When the map value type was changed from a single `InetAddress` to a `Collection<InetAddress>`, the call should have been updated to `endpoints.addAll(entry.getValue())`. Using `add` silently inserted only the first address or caused a compile error, so bootstrap nodes would not receive writes correctly.

## Example
```java
// BEFORE
for (Map.Entry<Range, InetAddress> entry : tokenMetadata_.getPendingRanges().entrySet())
{
    if (entry.getKey().contains(token))
        endpoints.add(entry.getValue());  // adds single address
}
```
```java
// AFTER
for (Map.Entry<Range, Collection<InetAddress>> entry : tokenMetadata_.getPendingRanges().entrySet())
{
    if (entry.getKey().contains(token))
        endpoints.addAll(entry.getValue());  // adds all addresses
}
```
