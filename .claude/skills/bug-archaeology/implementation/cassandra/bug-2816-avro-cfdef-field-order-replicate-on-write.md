# Bug 2816: Avro Schema Field Order Mismatch Causes replicate_on_write to Be Read as Wrong Type

**Commit**: 3e0df6ee0a · **Ticket**: CASSANDRA-2001

## Detection Rule
When an Avro schema defines fields in a specific positional order and Java deserialization code maps fields by position rather than name, adding a new field at the wrong position causes all subsequent fields to be deserialized with incorrect values or wrong types.

## Code Smell
`replicate_on_write` field in `internode.genavro`'s `CfDef` record is inserted before `gc_grace_seconds` rather than at the end, shifting all subsequent fields by one position. Avro uses positional field matching during deserialization for binary encoding.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: avro-field-order-mismatch
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause
The Avro-based internode schema `CfDef` record had `replicate_on_write` added between `read_repair_chance` and `gc_grace_seconds`. Avro binary encoding serializes fields positionally, so a receiver using a schema with `replicate_on_write` at one position and the sender using a schema with it at a different position would read all fields after the mismatch as the wrong type. The fix moves `replicate_on_write` to the end of the record (after `column_metadata`) where new fields should be added to maintain backward compatibility. Additionally, a null guard was added in `CFMetaData.fromAvro()` to handle `cf.replicate_on_write == null` by defaulting to `Boolean.FALSE`.

## Example
```avro
// BEFORE: replicate_on_write in wrong position (before gc_grace_seconds)
union { double, null } read_repair_chance;
union { boolean, null } replicate_on_write;  // wrong position
union { int, null } gc_grace_seconds;
```
```avro
// AFTER: replicate_on_write at end, after all existing fields
union { int, null } id;
union { array<ColumnDef>, null } column_metadata;
union { boolean, null } replicate_on_write;  // appended at end
```
