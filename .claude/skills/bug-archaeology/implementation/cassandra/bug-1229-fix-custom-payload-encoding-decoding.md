# Bug 1229: Custom Payload Encoding/Decoding Does Not Match Protocol Spec

**Commit**: 6f93bd1f65 · **Ticket**: CASSANDRA-9515
**Subsystem**: src/java/org/apache/cassandra/transport/CBUtil.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The native protocol specification defines custom payload values as `[bytes]`—length-prefixed byte sequences whose natural Java representation is `ByteBuffer`. The implementation typed them as `byte[]` throughout (`CBUtil`, `Message`, `QueryHandler`, `QueryProcessor`). When encoding, the code called `writeBytes(byte[])` instead of `writeValue(ByteBuffer)`, and when decoding, it called `readBytes(ByteBuf)` instead of `readValue(ByteBuf)`. These two wire formats differ in how the length prefix is interpreted, so payloads written by a client following the spec would be mis-decoded by the server and vice versa. The fix changes the map type to `Map<String, ByteBuffer>` everywhere and routes serialization through the spec-correct `readValue`/`writeValue`/`sizeOfValue` helpers.

### Before / After

```java
// BEFORE — wrong wire format (byte[] path uses a different length encoding)
public static Map<String, byte[]> readBytesMap(ByteBuf cb) {
    ...
    byte[] v = readBytes(cb);
    ...
}
public static void writeBytesMap(Map<String, byte[]> m, ByteBuf cb) {
    ...
    writeBytes(entry.getValue(), cb);
    ...
}
```

```java
// AFTER — correct wire format matching the protocol [bytes] type
public static Map<String, ByteBuffer> readBytesMap(ByteBuf cb) {
    ...
    ByteBuffer v = readValue(cb);
    ...
}
public static void writeBytesMap(Map<String, ByteBuffer> m, ByteBuf cb) {
    ...
    writeValue(entry.getValue(), cb);
    ...
}
```

---

## How to Spot

Whenever a protocol type is mapped to a Java type, verify that the serialisation helper chosen matches the protocol definition, not just what is convenient. Here `byte[]` and `ByteBuffer` look interchangeable at the Java level but route through different CBUtil methods that produce different wire bytes. A quick check is to confirm that the helper method name (`readBytes` vs `readValue`) corresponds to the protocol type name (`[short bytes]` vs `[bytes]`) in the spec.

---

## Test

`MessagePayloadTest` updated to use `ByteBuffer` values and the `bytes()` utility, exercising round-trip encoding for query, prepare, execute, and batch messages.
