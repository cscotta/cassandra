# Bug 2877: Reading beta1 Avro schema fails with NPE on null compaction thresholds and old strategy names

**Commit**: 009bd2c26a · **Ticket**: CASSANDRA-1545

## Detection Rule
When deserializing a versioned schema where older versions may omit optional fields, check that nullable Integer/String fields are replaced with defaults before being passed to constructors that expect non-null values.

## Code Smell
`CFMetaData.fromAvro` passes `cf.min_compaction_threshold` and `cf.max_compaction_threshold` directly to the constructor without null guards; Avro records from beta1 that lack these fields produce null `Integer` values which NPE when auto-unboxed. Similarly, old strategy class names that were renamed are not translated before `Class.forName`.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: nullable-avro-field-unboxed-without-default
- severity: crash
- subsystem: schema-metadata

## Root Cause
Beta1 Avro schema records did not include `min_compaction_threshold`, `max_compaction_threshold`, or the renamed replication strategy classes. On upgrade, loading these records produced NPEs or `ClassNotFoundException`. The fix adds null coalescing for the threshold fields and a `convertOldStrategyName` helper that maps `RackUnawareStrategy` → `SimpleStrategy`, `RackAwareStrategy` → `OldNetworkTopologyStrategy`, and `DatacenterShardStrategy` → `NetworkTopologyStrategy`.

## Example
```java
// BEFORE — NPE if cf.min_compaction_threshold is null
return new CFMetaData(..., cf.min_compaction_threshold, cf.max_compaction_threshold, ...);
```
```java
// AFTER
Integer minct = cf.min_compaction_threshold == null
    ? DEFAULT_MIN_COMPACTION_THRESHOLD : cf.min_compaction_threshold;
Integer maxct = cf.max_compaction_threshold == null
    ? DEFAULT_MAX_COMPACTION_THRESHOLD : cf.max_compaction_threshold;
return new CFMetaData(..., minct, maxct, ...);
```
