# Bug 2650: replicate_on_write defaults to false for counter column families due to missing isSet guard

**Commit**: 6ffe50521a · **Ticket**: CASSANDRA-2835

## Detection Rule
When applying defaults to a Thrift-generated object using `isSet` guards, include every safety-critical boolean flag — check `isSetReplicate_on_write()` and apply the correct default before using the value, rather than relying on the Java primitive default of `false`.

## Code Smell
`CFMetaData.fromThrift()` / `applyToCFMetaData()` applies defaults for `comment`, `min_compaction_threshold`, and `max_compaction_threshold` via `isSet` guards, but omits `replicate_on_write`; for Thrift clients that do not set this field the Java default (`false`) is used, which is unsafe for counter column families.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: missing-default-for-boolean-field
- severity: data-loss
- subsystem: schema-metadata

## Root Cause
When a `CfDef` from Thrift did not explicitly set `replicate_on_write`, the Thrift-generated class left it at the Java boolean default of `false`. `CFMetaData` consumed this value without checking `isSetReplicate_on_write()`, so any CF created or updated via a client that omitted the field (including Cassandra's own migration code in 0.8.0 and 0.8.1) would have `replicate_on_write=false`. For counter column families this is an unsafe setting that means counter writes are not replicated, risking data inconsistency. The fix adds an `isSet` guard and defaults to `CFMetaData.DEFAULT_REPLICATE_ON_WRITE` (true).

## Example
```java
// BEFORE
// no guard for replicate_on_write; Java default false is used silently
if (!cf_def.isSetMin_compaction_threshold())
    cf_def.setMin_compaction_threshold(CFMetaData.DEFAULT_MIN_COMPACTION_THRESHOLD);

// AFTER
if (!cf_def.isSetReplicate_on_write())
    cf_def.setReplicate_on_write(CFMetaData.DEFAULT_REPLICATE_ON_WRITE);
if (!cf_def.isSetMin_compaction_threshold())
    cf_def.setMin_compaction_threshold(CFMetaData.DEFAULT_MIN_COMPACTION_THRESHOLD);
```
