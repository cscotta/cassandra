# Bug 2229: Overriding extractFrame Caused Frame Data Corruption in Binary Protocol Decoder

**Commit**: 88ae22f385 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/transport/Frame.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`Frame.Decoder` overrode `extractFrame()` from Netty's `FrameDecoder` to return a zero-copy slice of the source buffer instead of a copy. Because `FrameDecoder` reuses and advances the underlying buffer between decode calls, returning a slice meant the extracted frame's content could be overwritten by subsequent incoming data before it was fully processed. Removing the override lets `FrameDecoder` use its default copy-based extraction, ensuring each frame holds its own independent buffer.

### Before / After

```java
// BEFORE — zero-copy slice shares buffer with live input stream
@Override
protected ChannelBuffer extractFrame(ChannelBuffer buffer, int index, int length)
{
    // Don't copy
    return buffer.slice(index, length);
}
```

```java
// AFTER — override removed; FrameDecoder copies the frame bytes by default
```

---

## How to Spot

Be wary of "don't copy" optimizations in streaming decoders that override buffer-extraction methods. If the parent class's contract requires an independent buffer (because it reuses the source buffer), a zero-copy slice will cause data corruption or non-deterministic behavior under load.

---

## Test

No test added.
