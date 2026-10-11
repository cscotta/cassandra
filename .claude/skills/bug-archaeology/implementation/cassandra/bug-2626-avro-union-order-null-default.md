# Bug 2626: Avro internode schema has wrong union order for row_cache_keys_to_save causing deserialization failure

**Commit**: 1a54114b3f · **Ticket**: n/a

## Detection Rule
When an Avro union field has a `null` default value, the `null` type must be the first member of the union; if a non-null type is first, Avro serializers will reject or misparse the field.

## Code Smell
A field declared as `union { int, null } field = null;` has `int` as the first union member but `null` as the default value, which violates the Avro spec and causes schema validation or deserialization errors when the field is absent.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: avro-union-order-mismatch
- severity: crash
- subsystem: network-messaging

## Root Cause
The internode Avro schema declared `row_cache_keys_to_save` as `union { int, null } = null`, placing `int` first while using `null` as the default. The Avro specification requires that for a union with a null default the `null` type must come first. This caused `SerializationsTest` to fail after commit #1966 and would produce deserialization errors on any node receiving an internode message containing this field.

## Example
```avro
// BEFORE
union { int, null } row_cache_keys_to_save = null;

// AFTER
union { null, int } row_cache_keys_to_save = null;
```
