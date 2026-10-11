# Bug 1963: Buffer underflow when reading StreamMessage after socket close

**Commit**: 22bf2c40e4 · **Ticket**: CASSANDRA-5792
**Subsystem**: src/java/org/apache/cassandra/streaming/messages/StreamMessage.java
**Category**: Missing null / bounds check

---

## Root Cause

`StreamMessage.deserialize` allocated a 1-byte `ByteBuffer` and passed it directly to `ReadableByteChannel.read()`. Per the Java NIO contract, `read()` returns 0 or a negative value when the channel is at end-of-stream (i.e., the remote side closed the socket), without writing any bytes into the buffer. The code then unconditionally called `buff.flip()` followed by `buff.get()`, which threw `BufferUnderflowException` because the buffer's position was still 0. The fix checks the return value of `read()` and returns `null` when no bytes were read, allowing the caller to handle the closed-channel case gracefully.

### Before / After

```java
// BEFORE
ByteBuffer buff = ByteBuffer.allocate(1);
in.read(buff);
buff.flip();
Type type = Type.get(buff.get());
return type.serializer.deserialize(in, version, session);
```

```java
// AFTER
ByteBuffer buff = ByteBuffer.allocate(1);
if (in.read(buff) > 0) {
    buff.flip();
    Type type = Type.get(buff.get());
    return type.serializer.deserialize(in, version, session);
} else {
    // when socket gets closed, there is a chance that buff is empty
    return null;
}
```

---

## How to Spot

Any NIO read loop that does not check the return value of `channel.read()` before consuming the buffer is potentially broken. End-of-stream and partial reads are distinct from a successful read; the byte count must be validated before `flip()` + `get()` are called.

---

## Test

No test added.
