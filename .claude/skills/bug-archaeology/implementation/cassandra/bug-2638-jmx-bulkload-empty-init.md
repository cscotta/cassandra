# Bug 2638: JMX bulkload init() is empty so range-to-endpoint mapping is never built

**Commit**: 5616b0235a · **Ticket**: CASSANDRA-2908

## Detection Rule
When an abstract `init()` method is implemented as a no-op in a concrete subclass that relies on the initialized state to route data, verify the implementation actually populates required mappings or the caller will silently send to no endpoints.

## Code Smell
The `SSTableLoader.Client` anonymous inner class used for JMX-triggered bulk loading overrides `init()` with an empty body, so `addRangeForEndpoint()` is never called and the loader has no idea where to stream SSTable data.

## Tags
- scope: cassandra
- category: silent-skip
- code-pattern: empty-init-implementation
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`StorageService.bulkLoad()` created an anonymous `SSTableLoader.Client` whose `init()` method was empty (`public void init() {}`). The `SSTableLoader` calls `init()` before streaming to allow the client to populate range-to-endpoint mappings. With an empty implementation, no ranges were registered, so the loader would stream to no nodes and the bulk load would silently succeed while writing nothing. The fix makes `init(String keyspace)` query `getRangeToAddressMap(keyspace)` and call `addRangeForEndpoint()` for every range.

## Example
```java
// BEFORE
public void init() {}  // no-op: range map never populated

// AFTER
public void init(String keyspace) {
    for (Map.Entry<Range, List<InetAddress>> entry :
            StorageService.instance.getRangeToAddressMap(keyspace).entrySet()) {
        Range range = entry.getKey();
        for (InetAddress endpoint : entry.getValue())
            addRangeForEndpoint(range, endpoint);
    }
}
```
