# Bug 2643: LOCAL_QUORUM reads send requests to remote DC endpoints causing spurious UnavailableException

**Commit**: 2d2105d6c4 · **Ticket**: CASSANDRA-2870

## Detection Rule
When building the endpoint list for a LOCAL_QUORUM read with read repair disabled, override `preferredEndpoints()` in the datacenter-aware callback subclass to filter for local-DC nodes only, rather than inheriting the base class's `subList(0, blockfor)` which picks by sort order without regard to datacenter.

## Code Smell
`ReadCallback` selects its request targets with `endpoints.subList(0, Math.min(endpoints.size(), blockfor))`, taking the first N endpoints from the global sorted list; `DatacenterReadCallback` inherits this and never restricts to the local datacenter, so when dynamic snitch reorders endpoints, remote-DC nodes can be selected and block for DC-local quorum.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-dc-filter-in-endpoint-selection
- severity: crash
- subsystem: cql-query

## Root Cause
`DatacenterReadCallback` is the LOCAL_QUORUM read callback, but it did not override `preferredEndpoints()`. It inherited the base `ReadCallback` implementation that selected the first `blockfor` endpoints from the global endpoint list sorted by dynamic snitch latency. When dynamic snitch placed remote-DC endpoints at the top of the list and read repair was disabled, the callback would request reads from those remote nodes, which were not counted toward local quorum. This caused the LOCAL_QUORUM check to find insufficient local responses and throw `UnavailableException` spuriously.

## Example
```java
// BEFORE – DatacenterReadCallback inherits base, selects by latency order (may pick remote DC)
// no override of preferredEndpoints()

// AFTER – filter to local DC only
@Override
protected List<InetAddress> preferredEndpoints(List<InetAddress> endpoints) {
    ArrayList<InetAddress> preferred = new ArrayList<>(blockfor);
    for (InetAddress endpoint : endpoints) {
        if (localdc.equals(snitch.getDatacenter(endpoint)))
            preferred.add(endpoint);
        if (preferred.size() == blockfor) break;
    }
    return preferred;
}
```
