# Bug 2669: Inconsistency window during bootstrap

**Commit**: 31f0ee95e9 · **Ticket**: CASSANDRA-833

## Detection Rule
When a bootstrapping node must be included in write quorum calculations, the pending endpoints must be tracked separately from natural endpoints so that write-response handlers can correctly compute how many acknowledgements to wait for.

## Code Smell
`getWriteEndpoints()` returning only a merged `Set<InetAddress>` of natural + pending endpoints, with no way for callers to distinguish which are pending—so write-response handlers cannot increase their required acknowledgement count to cover bootstrapping nodes.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-pending-endpoint-tracking
- severity: silent-wrong-result
- subsystem: bootstrap-startup

## Root Cause
During bootstrap a new node registers pending ranges and must receive writes before it is fully live so it does not miss data written in the bootstrap window. The old code merged pending endpoints into the natural set and returned a single collection, making them indistinguishable. Write-response handlers did not know to wait for the pending nodes, creating a window where acknowledged writes could be missing from the bootstrapping node. The fix returns a `Pair` of (all write endpoints, pending-only endpoints) and threads the pending set through all write-response handler factories so they can inflate their required-ack count accordingly.

## Example
```java
// BEFORE – returns merged set, loses pending identity
public Collection<InetAddress> getWriteEndpoints(Token token, String table,
        Collection<InetAddress> naturalEndpoints) {
    Set<InetAddress> endpoints = new HashSet<>(naturalEndpoints);
    for (Map.Entry<Range, Collection<InetAddress>> entry : ranges.entrySet())
        if (entry.getKey().contains(token))
            endpoints.addAll(entry.getValue());
    return endpoints;
}
```
```java
// AFTER – returns Pair<allEndpoints, pendingEndpoints>
public Pair<? extends Iterable<InetAddress>, ? extends Iterable<InetAddress>>
        getWriteEndpoints(...) {
    Set<InetAddress> pendings = new HashSet<>();
    for (Map.Entry<Range, Collection<InetAddress>> entry : ranges.entrySet())
        if (entry.getKey().contains(token))
            pendings.addAll(entry.getValue());
    return Pair.create(Iterables.concat(naturalEndpoints, pendings), pendings);
}
```
