# Bug 2050: RowMutation Serializes Keyspace Name Redundantly in Version 2.0+

**Commit**: f04359d91a · **Ticket**: CASSANDRA-5458
**Subsystem**: src/java/org/apache/cassandra/db/RowMutation.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`RowMutation` serialized the keyspace name as a UTF string for all protocol versions, even though in version 2.0+ the keyspace can be recovered from the column family metadata embedded in the payload. This wasted bytes on the wire and also caused deserialization to attempt reading a keyspace field that was not present in version-2.0 messages produced by other nodes. Additionally, the constructor `RowMutation(ByteBuffer key, ColumnFamily cf)` retrieved the keyspace via `Schema.instance.getCFMetaData(cf.id()).ksName` instead of the more direct `cf.metadata().ksName`, adding an unnecessary schema lookup.

### Before / After

```java
// BEFORE (serialize)
out.writeUTF(rm.getTable());
ByteBufferUtil.writeWithShortLength(rm.key(), out);

// BEFORE (deserialize)
String table = in.readUTF();
ByteBuffer key = ByteBufferUtil.readWithShortLength(in);
```

```java
// AFTER (serialize)
if (version < MessagingService.VERSION_20)
    out.writeUTF(rm.getTable());
ByteBufferUtil.writeWithShortLength(rm.key(), out);

// AFTER (deserialize)
String table = null;
if (version < MessagingService.VERSION_20)
    table = in.readUTF();
// ... table is set from cf.metadata().ksName after reading modifications
```

---

## How to Spot

When adding a version-gated protocol change, audit both the `serialize` and `deserialize` paths together to ensure they are symmetric for every supported version. Also verify that `serializedSize` accounts for the same conditional logic, otherwise size assertions will fail.

---

## Test

No dedicated new test added; an existing serialization regression test (`SerializationsTest`) was updated to reflect the new wire format.
