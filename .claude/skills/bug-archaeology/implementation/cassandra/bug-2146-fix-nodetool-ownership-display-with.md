# Bug 2146: nodetool Ownership Percentages Wrong With Virtual Nodes

**Commit**: d67816bee5 · **Ticket**: CASSANDRA-5065
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`getOwnership()` iterated over the per-token ownership map and stored each token's fractional ownership in a node-keyed result map, but used a plain `put` that overwrote the previous entry. With virtual nodes (vnodes), a single endpoint owns many tokens, so only the last token's share was kept for each node rather than the sum of all token shares. The displayed ownership was therefore far too low for any node holding more than one token.

### Before / After

```java
// BEFORE – last token wins, earlier tokens discarded
for (Map.Entry<Token, Float> entry : tokenMap.entrySet())
    stringMap.put(tokenMetadata.getEndpoint(entry.getKey()), entry.getValue());
```

```java
// AFTER – accumulate ownership across all tokens for the same endpoint
for (Map.Entry<Token, Float> entry : tokenMap.entrySet())
{
    InetAddress endpoint = tokenMetadata.getEndpoint(entry.getKey());
    Float tokenOwnership = entry.getValue();
    if (nodeMap.containsKey(endpoint))
        nodeMap.put(endpoint, nodeMap.get(endpoint) + tokenOwnership);
    else
        nodeMap.put(endpoint, tokenOwnership);
}
```

---

## How to Spot

When building an aggregated map from a source that can have multiple entries per key (many tokens per node in a vnode ring), a plain `Map.put` silently drops all but the last value. Review any loop that writes into a map keyed by a coarser entity than the loop variable.

---

## Test

No test added.
