# Bug 2636: Updating a keyspace silently resets durable_writes to default because it is not forwarded through migration

**Commit**: d36dd2fcba · **Ticket**: CASSANDRA-2907

## Detection Rule
When constructing an updated schema object from an existing one, explicitly pass every mutable field — including boolean flags like `durable_writes` — to the constructor rather than relying on defaults; and use generated setters rather than direct field assignment so the Thrift "isSet" tracking is updated.

## Code Smell
`UpdateKeyspace` creates `newKsm` with `new KSMetaData(ksm.name, ksm.strategyClass, ksm.strategyOptions, oldKsm.cfMetaData()...)` without passing `ksm.isDurableWrites()`, silently resetting `durable_writes` to its default; separately, `KSMetaData.toThrift()` uses `ksdef.durable_writes = ...` instead of `ksdef.setDurable_writes(...)`, leaving the "isSet" bit unset.

## Tags
- scope: cassandra
- category: silent-skip
- code-pattern: field-not-forwarded-in-update
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause
`UpdateKeyspace` reconstructed the new `KSMetaData` object without supplying the `durable_writes` boolean from the incoming definition, so every keyspace update operation reset the flag to the Java default. Additionally, `KSMetaData.toThrift()` assigned `ksdef.durable_writes` via direct field access instead of `ksdef.setDurable_writes()`, so the Thrift-generated "isSet" flag was never set, causing serialization to omit the field. Both bugs together meant `durable_writes=false` could never be persisted through a keyspace update.

## Example
```java
// BEFORE
this.newKsm = new KSMetaData(ksm.name, ksm.strategyClass, ksm.strategyOptions,
    oldKsm.cfMetaData().values().toArray(...));  // durable_writes lost
ksdef.durable_writes = ksm.durable_writes;      // isSet flag not updated

// AFTER
this.newKsm = new KSMetaData(ksm.name, ksm.strategyClass, ksm.strategyOptions,
    ksm.isDurableWrites(), oldKsm.cfMetaData().values().toArray(...));
ksdef.setDurable_writes(ksm.durable_writes);    // isSet flag set correctly
```
