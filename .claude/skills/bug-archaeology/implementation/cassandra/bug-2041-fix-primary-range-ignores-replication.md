# Bug 2041: Primary Range Calculation Ignores Replication Strategy

**Commit**: debd8f0176 · **Ticket**: CASSANDRA-5424
**Subsystem**: src/java/org/apache/cassandra/locator/NetworkTopologyStrategy.java · src/java/org/apache/cassandra/service/StorageService.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`getPrimaryRangesForEndpoint` (and the `getLocalPrimaryRanges` helper it backed) computed primary ranges purely from the token ring topology — the set of tokens assigned to the endpoint — without consulting the keyspace's replication strategy. With `NetworkTopologyStrategy` configured to replicate only to certain datacenters, nodes in a non-replicated DC would receive non-empty primary ranges, and nodes in a DC that is the sole replica source would miss ranges that they should own. The fix rewrites the method to iterate all tokens, call `calculateNaturalEndpoints` for each, and credit a range to the endpoint only when that endpoint appears first in the endpoint list.

Additionally, `NetworkTopologyStrategy.calculateNaturalEndpoints` used a plain `HashSet` for the replica set, losing insertion order and therefore making "first endpoint = primary" semantics unreliable. It was changed to `LinkedHashSet` to preserve insertion order.

### Before / After

```java
// BEFORE
public Collection<Range<Token>> getPrimaryRangesForEndpoint(InetAddress ep)
{
    return tokenMetadata.getPrimaryRangesFor(tokenMetadata.getTokens(ep));
}
```

```java
// AFTER
public Collection<Range<Token>> getPrimaryRangesForEndpoint(String keyspace, InetAddress ep)
{
    AbstractReplicationStrategy strategy = Table.open(keyspace).getReplicationStrategy();
    Collection<Range<Token>> primaryRanges = new HashSet<Range<Token>>();
    TokenMetadata metadata = tokenMetadata.cloneOnlyTokenMap();
    for (Token token : metadata.sortedTokens())
    {
        List<InetAddress> endpoints = strategy.calculateNaturalEndpoints(token, metadata);
        if (endpoints.size() > 0 && endpoints.get(0).equals(ep))
            primaryRanges.add(new Range<Token>(metadata.getPredecessor(token), token));
    }
    return primaryRanges;
}
```

```java
// NetworkTopologyStrategy — BEFORE
Set<InetAddress> replicas = new HashSet<InetAddress>();
```

```java
// NetworkTopologyStrategy — AFTER
// we want to preserve insertion order so that the first added endpoint becomes primary
Set<InetAddress> replicas = new LinkedHashSet<InetAddress>();
```

---

## How to Spot

Any time repair or key-sampling logic calls a "primary range" helper, verify that the helper passes the keyspace name and delegates to the actual replication strategy rather than relying purely on ring-topology arithmetic. Look for calls that take only an endpoint address and check whether the corresponding implementation consults `ReplicationStrategy`.

---

## Test

`StorageServiceServerTest` was extended with four new test methods covering NTS with two DCs, NTS with replication in only one DC, NTS with vnodes, and `SimpleStrategy`. The single-DC NTS test explicitly verifies that nodes in the non-replicated DC return empty primary-range collections.
