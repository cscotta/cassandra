# Bug 2715: Schema migration from 0.7 loses replication_factor for SimpleStrategy keyspaces

**Commit**: 977e48294b · **Ticket**: CASSANDRA-2450

## Detection Rule
When deserializing a versioned schema over the wire, check whether optional legacy fields (absent in the new schema but present in old nodes) need to be promoted into the current model.

## Code Smell
Avro `KsDef` deserialization initializes `strategyOptions` to `null` when `strategy_options` is absent, then drops the legacy `replication_factor` integer field, so keyspaces streaming from 0.7 nodes silently have no replication factor.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: missing-legacy-field-promotion
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause
The internode Avro `KsDef` schema gained a `strategy_options` map in 0.8 to replace the flat `replication_factor` integer, but 0.7 nodes still send the old field. `KSMetaData.fromAvro()` only copied entries from `strategy_options` when non-null, ignoring the `replication_factor` field entirely. A keyspace received from a 0.7 peer therefore had an empty strategy-options map and defaulted to RF=0. The fix always initialises the options map to an empty `HashMap`, then checks `ks.replication_factor != null` and injects it as `"replication_factor"` into the map.

## Example
```java
// BEFORE — replication_factor integer silently discarded
Map<String, String> strategyOptions = null;
if (ks.strategy_options != null) {
    strategyOptions = new HashMap<>();
    for (...) { strategyOptions.put(...); }
}
```
```java
// AFTER — legacy integer promoted into the options map
Map<String, String> strategyOptions = new HashMap<>();
if (ks.strategy_options != null) {
    for (...) { strategyOptions.put(...); }
}
if (ks.replication_factor != null)
    strategyOptions.put("replication_factor", ks.replication_factor.toString());
```
