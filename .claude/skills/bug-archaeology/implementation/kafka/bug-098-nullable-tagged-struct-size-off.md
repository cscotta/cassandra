**Commit**: b94defa189 · **Ticket**: KAFKA-18199
**Subsystem**: generator/src/main/java/org/apache/kafka/message/MessageDataGenerator.java
**Category**: Wrong serialization / deserialization

**Root cause**: For tagged nullable struct fields the wire format is `{varint tag; varint dataLength; is-not-null byte; struct bytes}`. The `dataLength` varint must encode the length of *everything that follows it* including the is-not-null byte. In the generated `addSize` method, the is-not-null byte was added to the running size total *before* `_sizeBeforeStruct` was captured, so the `_structSize` calculation excluded it. The varint size of `_structSize` was therefore computed as if the struct were one byte smaller than the length actually written, causing a size/write mismatch. The bug only materialized when `dataLength` was exactly 127 (the boundary between 1- and 2-byte varints), producing a buffer that was one byte too short, corrupting deserialization of the field.

**Before/After**

```java
// Before – nullable byte counted before snapshot, excluded from structSize varint calc
_size.addBytes(1);                       // nullable byte added first
int _sizeBeforeStruct = _size.totalSize(); // snapshot taken after
this.field.addSize(_size, _cache, _version);
int _structSize = _size.totalSize() - _sizeBeforeStruct; // excludes nullable byte
_size.addBytes(ByteUtils.sizeOfUnsignedVarint(_structSize)); // wrong varint size

// After – snapshot taken first, nullable byte included in structSize
int _sizeBeforeStruct = _size.totalSize(); // snapshot first
_size.addBytes(1);                         // nullable byte now inside structSize window
this.field.addSize(_size, _cache, _version);
int _structSize = _size.totalSize() - _sizeBeforeStruct; // includes nullable byte
_size.addBytes(ByteUtils.sizeOfUnsignedVarint(_structSize)); // correct
```

**How to spot**: When computing the size of a variable-length field that contains a fixed prefix (like the is-not-null flag), the snapshot for "bytes before this field" must be taken *before* adding the prefix. Order matters because the prefix contributes to the length varint.

**Test**: `testTaggedStructSize` — crafts a `NullableStructMessageData` whose tagged struct is exactly 127 bytes (one byte before the varint overflow boundary), round-trips it, and asserts the serialised size matches `message.size()`.
