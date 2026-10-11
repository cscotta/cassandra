# Bug 2782: Streaming Messages Fail to Deserialize OperationType on 0.7 Wire Protocol

**Commit**: 75148e12e9 · **Ticket**: CASSANDRA-1949

## Detection Rule
When adding a new field to a serialized message format, guard its write and read with a version check so that older nodes running the prior protocol version do not encounter an unexpected field.

## Code Smell
`dos.writeUTF(sc.type.name())` and `OperationType.valueOf(dis.readUTF())` are called unconditionally in `PendingFile` and `StreamRequestMessage` serializers, but the `OperationType` field did not exist in the 0.7 protocol (version 1).

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: missing-version-guard-for-new-field
- severity: crash
- subsystem: streaming

## Root Cause
The `OperationType` enum field was added to `PendingFile` and `StreamRequestMessage` without versioning the serialization. During a rolling upgrade from 0.7 (messaging version 1) to 0.8, nodes running 0.7 would receive messages with an extra `OperationType` field they didn't know about, and 0.8 nodes reading 0.7 messages would try to `readUTF` a field that was never written. The fix introduces `MessagingService.VERSION_07 = 1` and gates the field on `version > VERSION_07`.

## Example
```java
// BEFORE (serializer, no version check)
dos.writeUTF(sc.type.name());
// ...
OperationType type = OperationType.valueOf(dis.readUTF());
```
```java
// AFTER
if (version > MessagingService.VERSION_07)
    dos.writeUTF(sc.type.name());
// ...
OperationType type = OperationType.RESTORE_REPLICA_COUNT;
if (version > MessagingService.VERSION_07)
    type = OperationType.valueOf(dis.readUTF());
```
