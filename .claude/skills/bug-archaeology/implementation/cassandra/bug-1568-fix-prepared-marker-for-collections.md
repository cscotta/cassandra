# Bug 1568: Fix prepared marker for collections inside UDT

**Commit**: 9b539f966f · **Ticket**: CASSANDRA-7472
**Subsystem**: cql3 / UserTypes, Tuples, CollectionSerializer
**Category**: Wrong serialization / deserialization

---

## Root Cause

When a client using the native protocol v1 or v2 executes a prepared statement that stores a collection value inside a User-Defined Type (UDT) or tuple, the collection must be serialized in the v3 wire format because that is the on-disk representation. The original approach forced this by replacing the entire `QueryOptions` object with a v3 clone (via `withProtocolVersion(3)`), which caused incorrect behavior in other parts of query execution that legitimately needed the real protocol version.

The fix removes `withProtocolVersion` entirely and instead re-serializes only the collection fields that are inside UDT/tuple values: after binding each element, if the client protocol version is less than 3 and the field type is a collection, the already-bound `ByteBuffer` is re-serialized from v2 format to v3 format via a new `reserializeToV3` helper.

### Before / After

```java
// BEFORE (UserTypes.java / Tuples.java bindInternal)
// Blindly upgraded the whole options object to v3 before binding
options = options.withProtocolVersion(3);
ByteBuffer[] buffers = new ByteBuffer[values.size()];
for (int i = 0; i < type.size(); i++)
    buffers[i] = values.get(i).bindAndGet(options);
```

```java
// AFTER — bind with the real options, then re-serialize collections only
int version = options.getProtocolVersion();
ByteBuffer[] buffers = new ByteBuffer[values.size()];
for (int i = 0; i < type.size(); i++)
{
    buffers[i] = values.get(i).bindAndGet(options);
    if (version < 3 && type.fieldType(i).isCollection())
        buffers[i] = ((CollectionType)type.fieldType(i)).getSerializer().reserializeToV3(buffers[i]);
}
```

---

## How to Spot

When protocol-version-sensitive serialization is needed only for a subset of fields (those inside a container type), avoid replacing the global options object. Instead apply the conversion selectively on the affected fields after normal binding, preserving the original options for all other code that depends on the real protocol version.

---

## Test

No test added in this commit.
