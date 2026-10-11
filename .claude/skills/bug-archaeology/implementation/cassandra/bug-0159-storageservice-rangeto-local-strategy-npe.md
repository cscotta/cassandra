# Bug 0159: getRangeTo* StorageService MBean Crashes for LocalStrategy Keyspaces

**Commit**: a69c8657d7 · **Ticket**: CASSANDRA-19255
**Subsystem**: `service/StorageService`
**Category**: Missing null / bounds check

---

## Root Cause

`StorageService.constructRangeToEndpointMap()` unconditionally looked up `keyspaceMetadata.params.replication` in `metadata.placements` to resolve endpoints per range. Keyspaces that use `LocalStrategy` (e.g., `system`) are not managed by the Cluster Metadata Service (CMS) and have no entry in `metadata.placements`, so `keyspaceMetadata` came back `null` from the CMS schema lookup, causing an NPE. The fix adds a null check on `keyspaceMetadata`: when null, the method falls back to opening the `Keyspace` directly and using its `ReplicationStrategy` to compute natural replicas.

### Before / After

```java
// BEFORE — NPE when keyspaceMetadata is null (LocalStrategy keyspace)
KeyspaceMetadata keyspaceMetadata = metadata.schema.getKeyspaces().getNullable(keyspace);
TokenMap tokenMap = metadata.tokenMap;
for (Range<Token> range : ranges) {
    Token token = tokenMap.nextToken(tokenMap.tokens(), range.right.getToken());
    rangeToEndpointMap.put(range,
        metadata.placements.get(keyspaceMetadata.params.replication)  // NPE here
                           .reads.forRange(token).get());
}
```

```java
// AFTER — branches on keyspaceMetadata nullness
if (null != keyspaceMetadata) {
    TokenMap tokenMap = metadata.tokenMap;
    for (Range<Token> range : ranges) {
        Token token = tokenMap.nextToken(tokenMap.tokens(), range.right.getToken());
        rangeToEndpointMap.put(range, metadata.placements.get(keyspaceMetadata.params.replication)
                                              .reads.forRange(token).get());
    }
} else {
    AbstractReplicationStrategy strategy = Keyspace.open(keyspace).getReplicationStrategy();
    for (Range<Token> range : ranges)
        rangeToEndpointMap.put(range, strategy.calculateNaturalReplicas(range.right, metadata));
}
```

---

## How to Spot

Calls to `schema.getKeyspaces().getNullable(ks)` that immediately dereference the result without a null check are always suspect. This pattern is especially dangerous in JMX/MBean paths that accept arbitrary user-supplied keyspace names, since any keyspace outside CMS control (LocalStrategy) or a misspelled name will return null.

---

## Test

`StorageServiceJmxTest.testGetRangeToEndpointMap` invokes all four `getRangeTo*` JMX operations against four keyspace types (LocalStrategy, SimpleStrategy, NetworkTopologyStrategy, MetaStrategy) and asserts each returns a non-empty non-null map.
