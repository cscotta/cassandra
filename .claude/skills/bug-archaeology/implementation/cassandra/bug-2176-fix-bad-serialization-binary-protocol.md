# Bug 2176: ReadUnavailableException Serialized With Wrong Buffer Size in Binary Protocol

**Commit**: 9c5e5dc0db · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/transport/messages/ErrorMessage.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When serializing a `ReadTimeoutException` over the binary protocol, the code allocated a `ChannelBuffer` sized using `writableBytes()` on an already-consumed (read) buffer `rteCl`, instead of `readableBytes()`. After `rteCl` had been written and its read position advanced, `writableBytes()` returns the remaining capacity beyond the current write position (or zero if fully written), which is far smaller than the actual data. This caused the serialized error message to be truncated or corrupted.

### Before / After

```java
// BEFORE
acb = ChannelBuffers.buffer(rteCl.writableBytes() + 8 + extraSize);
```

```java
// AFTER
acb = ChannelBuffers.buffer(rteCl.readableBytes() + 8 + extraSize);
```

---

## How to Spot

When computing a buffer size from an existing `ChannelBuffer` that is about to be read (via `writeBytes`), always use `readableBytes()`. Using `writableBytes()` is correct only when you need to know how much more you can write into the buffer, not how much data is available to read.

---

## Test

No test added.
