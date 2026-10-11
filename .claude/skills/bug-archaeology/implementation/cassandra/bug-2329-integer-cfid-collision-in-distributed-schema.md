# Bug 2329: Integer CF IDs cause conflicts when concurrent schema changes assign the same sequential ID

**Commit**: 90170d1594 · **Ticket**: CASSANDRA-3794

## Detection Rule

When schema objects are assigned sequential integer IDs, check whether concurrent creates on different nodes can generate the same ID and whether the ID-to-schema mapping survives a drop-and-recreate with the same name.

## Code Smell

A `nextCFId()` counter that increments a JVM-local integer and uses it as the column family identifier — different nodes generating independent sequences will produce identical IDs for unrelated tables, and schemas shared via gossip/migrations will collide.

## Tags

- scope: distributed-systems
- category: race-condition
- code-pattern: sequential-id-in-distributed-system
- severity: data-loss
- subsystem: schema-metadata

## Root Cause

Column family IDs were `Integer` values assigned by a per-node incrementing counter. When two nodes concurrently created different column families, they could assign the same integer ID. Because row cache keys, serialization, and schema lookups all used the CF ID, a collision caused one node's schema state to overwrite the other's — potentially misrouting reads/writes to the wrong column family. The fix changes CF IDs to type-3 UUIDs derived deterministically from `(keyspace, cfName)` using `UUID.nameUUIDFromBytes`, making them globally unique and reproducible, and adds an old-integer-to-UUID mapping for backward compatibility with Avro-serialized schemas.

## Example

```java
// BEFORE: sequential integer, local to each JVM
public final Integer cfId;
public CFMetaData(String keyspace, String name, ...) {
    this(keyspace, name, ..., Schema.instance.nextCFId());  // could collide across nodes
}
```

```java
// AFTER: deterministic UUID from (keyspace, cfName)
public final UUID cfId;
static UUID getId(String ksName, String cfName) {
    return UUID.nameUUIDFromBytes(ArrayUtils.addAll(ksName.getBytes(), cfName.getBytes()));
}
public CFMetaData(String keyspace, String name, ...) {
    this(keyspace, name, ..., getId(keyspace, name));
}
```
