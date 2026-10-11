# Bug 2519: row_cache_provider field not copied in CFMetaData.convertToThrift

**Commit**: 7a21ad2704 · **Ticket**: CASSANDRA-3414

## Detection Rule
When a method serializes a `CFMetaData` to its Thrift/Avro representation by copying fields one-by-one, check that all fields present in the schema are included; a missing field is silently dropped and the update appears to succeed.

## Code Smell
`convertToThrift()` copies `key_alias`, `merge_shards_chance`, and other fields but is missing `newDef.row_cache_provider = def.row_cache_provider` — any ALTER TABLE that changes only `row_cache_provider` is serialized as if the field kept its default value.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: field-omitted-from-serialization
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause
`CFMetaData.convertToThrift()` (used when persisting schema changes) copied every field from the internal `CFMetaData` object to the Thrift `CfDef` representation except `row_cache_provider`. Any schema migration that touched `row_cache_provider` appeared to succeed but the value was never stored, causing the node to restart with the wrong (default) cache provider for affected column families.

## Example
```java
// BEFORE — row_cache_provider silently omitted
newDef.subcomparator_type = def.getSubcomparator_type();
newDef.merge_shards_chance = def.getMerge_shards_chance();
newDef.key_alias = def.key_alias;
// row_cache_provider missing
```
```java
// AFTER
newDef.subcomparator_type = def.getSubcomparator_type();
newDef.merge_shards_chance = def.getMerge_shards_chance();
newDef.key_alias = def.key_alias;
newDef.row_cache_provider = def.row_cache_provider;
```
