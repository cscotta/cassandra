# Bug 1729: Repairing With Wrong Nodes Per Range

**Commit**: 84626372c0 · **Ticket**: CASSANDRA-6808
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When running incremental repair across multiple token ranges, the code collected all neighbors for all ranges into a single flat `Set<InetAddress>`. Each individual repair session then received this union set rather than the subset of neighbors that actually own that specific range. As a result, anti-entropy sessions could be started with nodes that do not replicate a given range, causing incorrect repair coordination and potentially marking SSTables as repaired against the wrong replica set.

### Before / After

```java
// BEFORE
Set<InetAddress> neighbours = new HashSet<>();
for (Range<Token> range : ranges)
    neighbours.addAll(ActiveRepairService.getNeighbors(keyspace, range, ...));
// ...
future = forceKeyspaceRepair(parentSession, range, keyspace, isSequential, neighbours, ...);
```

```java
// AFTER
Set<InetAddress> allNeighbors = new HashSet<>();
Map<Range, Set<InetAddress>> rangeToNeighbors = new HashMap<>();
for (Range<Token> range : ranges)
{
    Set<InetAddress> neighbors = ActiveRepairService.getNeighbors(keyspace, range, ...);
    rangeToNeighbors.put(range, neighbors);
    allNeighbors.addAll(neighbors);
}
// ...
future = forceKeyspaceRepair(parentSession, range, keyspace, isSequential, rangeToNeighbors.get(range), ...);
```

---

## How to Spot

When iterating over a collection of entities that each require a per-item lookup, be wary of accumulating results into a single shared set and then passing that entire set where a per-item subset is expected. Review every downstream use of the accumulated variable to confirm it is appropriate for all callers.

---

## Test

No test added.
