# Bug 2328: RowMutation serialization writes CF ID twice (once redundantly from ColumnFamily.serializer)

**Commit**: b4d94c0e7c · **Ticket**: CASSANDRA-4293

## Detection Rule

When a serialization format is changed so that a sub-serializer (e.g., `ColumnFamily.serializer`) already writes an identifier, verify that the outer serializer does not also write the same identifier separately — duplication causes deserialization to read one too many bytes and misparse the stream.

## Code Smell

Two consecutive calls during serialization — `ColumnFamily.serializer.serializeCfId(entry.getKey(), ...)` followed immediately by `ColumnFamily.serializer.serialize(entry.getValue(), ...)` — where the `serialize()` call in the new protocol version already includes the CF ID internally.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: double-write-of-field-in-updated-protocol
- severity: crash
- subsystem: network-messaging

## Root Cause

In protocol version 12, `ColumnFamily.serializer.serialize()` was updated to include the CF ID as part of the column family's own wire encoding. However, the `RowMutation` serializer was not updated and still explicitly called `serializeCfId()` before calling `serialize()`. This resulted in the CF ID being written twice for VERSION_12+, causing deserialization failures on the receiving end. The fix gates the explicit `serializeCfId()` / `deserializeCfId()` calls on `version < MessagingService.VERSION_12`.

## Example

```java
// BEFORE: always serializes CF ID separately, then again inside serialize()
ColumnFamily.serializer.serializeCfId(entry.getKey(), dos, version);  // double-write for v12+
ColumnFamily.serializer.serialize(entry.getValue(), dos, version);
```

```java
// AFTER: explicit CF ID write only for older protocol versions
if (version < MessagingService.VERSION_12)
    ColumnFamily.serializer.serializeCfId(entry.getKey(), dos, version);
ColumnFamily.serializer.serialize(entry.getValue(), dos, version);
// On read: cf.id() used directly instead of separately deserialized cfId
```
