# Bug 2455: Avro deserialization NPE when "caching" field is absent in internode messages

**Commit**: bcc87a9ee8 · **Ticket**: CASSANDRA-3143

## Detection Rule

When deserializing an Avro (or similar schema-evolved) record that has a nullable union field, guard against null before calling methods on the resulting object.

## Code Smell

`Caching.fromString(cf.caching.toString())` called unconditionally where `cf.caching` is a `union { null, string }` Avro field that can be null after schema evolution; this throws an NPE when older nodes send messages without the field.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: nullable-avro-field-no-null-guard
- severity: crash
- subsystem: network-messaging

## Root Cause

The Avro schema for `CFDef` defined `caching` as `string caching = "keys_only"` (non-nullable). When CASSANDRA-3667 added a new `caching` parameter, the field was accessed but without guarding against null. After a schema change that made it `union { null, string } caching = null`, older inter-node messages omit the field and it deserializes as null. Calling `.toString()` on null caused an NPE during schema exchange. The fix checks for null and falls back to `Caching.KEYS_ONLY`.

## Example

```java
// BEFORE – NPE if cf.caching is null (absent in older message)
caching = Caching.fromString(cf.caching.toString());

// AFTER
caching = cf.caching == null ? Caching.KEYS_ONLY : Caching.fromString(cf.caching.toString());
```
